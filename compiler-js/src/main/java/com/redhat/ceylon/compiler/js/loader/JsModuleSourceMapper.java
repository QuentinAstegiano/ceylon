package com.redhat.ceylon.compiler.js.loader;

import static com.redhat.ceylon.model.typechecker.model.Module.LANGUAGE_MODULE_NAME;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.redhat.ceylon.cmr.api.ArtifactContext;
import com.redhat.ceylon.cmr.impl.AbstractRepository;
import com.redhat.ceylon.cmr.resolver.javascript.JavaScriptResolver;
import com.redhat.ceylon.common.Backends;
import com.redhat.ceylon.common.ModuleUtil;
import com.redhat.ceylon.common.Versions;
import com.redhat.ceylon.common.config.CeylonConfig;
import com.redhat.ceylon.common.config.DefaultToolOptions;
import com.redhat.ceylon.compiler.js.CeylonRunJsException;
import com.redhat.ceylon.compiler.js.CompilerErrorException;
import com.redhat.ceylon.compiler.typechecker.analyzer.ModuleSourceMapper;
import com.redhat.ceylon.compiler.typechecker.context.Context;
import com.redhat.ceylon.compiler.typechecker.context.PhasedUnits;
import com.redhat.ceylon.model.cmr.ArtifactResult;
import com.redhat.ceylon.model.typechecker.model.Module;
import com.redhat.ceylon.model.typechecker.model.ModuleImport;
import com.redhat.ceylon.model.typechecker.util.ModuleManager;

public class JsModuleSourceMapper extends ModuleSourceMapper {

    /** Tells whether the language module has been loaded yet. */
    private boolean clLoaded;
    private String encoding;

    public JsModuleSourceMapper(Context context, ModuleManager moduleManager, String encoding) {
        super(context, moduleManager);
        this.encoding = encoding;
    }

    protected void loadModuleFromMap(ArtifactResult artifact, Module module, LinkedList<Module> dependencyTree,
            List<PhasedUnits> phasedUnitsOfDependencies, boolean forCompiledModule,
            Map<String, Object> model) {
        @SuppressWarnings("unchecked")
        List<Object> deps = (List<Object>)model.get("$mod-deps");
        if (deps != null) {
            for (Object dep : deps) {
                final String s;
                boolean optional = false;
                boolean export = false;
                if (dep instanceof Map) {
                    @SuppressWarnings("unchecked")
                    final Map<String,Object> depmap = (Map<String,Object>)dep;
                    s = (String)depmap.get("path");
                    optional = depmap.containsKey("opt");
                    export = depmap.containsKey("exp");
                } else {
                    s = (String)dep;
                }
                int p = s.indexOf('/');
                String depuri = null;
                String depv = null;
                if (p > 0) {
                    depuri = s.substring(0,p);
                    depv = s.substring(p+1);
                    if (depv.isEmpty()) {
                        depv = null;
                    }
                    //TODO Remove this hack after next bin compat breaks
                    if (LANGUAGE_MODULE_NAME.equals(depuri)) {
                        if ("1.1.0".equals(depv)) {
                            depv = "1.2.0";
                        } else if ("1.2.1".equals(depv)) {
                            depv = "1.3.2-SNAPSHOT";
                        } else if ("1.2.2".equals(depv)) {
                            depv = "1.3.2-SNAPSHOT";
                        } else if ("1.3.0".equals(depv)) {
                            depv = "1.3.2-SNAPSHOT";
                        }
                    }
                } else {
                    depuri = s;
                }
                String depnamespace = ModuleUtil.getNamespaceFromUri(depuri);
                String depname = ModuleUtil.getModuleNameFromUri(depuri);
                //This will cause the dependency to be loaded later
                JsonModule mod = (JsonModule)getModuleManager().getOrCreateModule(
                        ModuleManager.splitModuleName(depname), depv);
                Backends backends = mod.getNativeBackends();
                ModuleImport imp = new ModuleImport(depnamespace, mod, optional, export, backends);
                module.addImport(imp);
            }
            model.remove("$mod-deps");
        }
        ((JsonModule)module).setModel(model);
        ((JsonModule)module).loadDeclarations();
        return;
    }

    /** Read the metamodel declaration from a js file,
     * check it's the right version and return the model as a Map. */
    public static Map<String,Object> loadJsonModel(File jsFile) {
        try {
            Map<String,Object> model = JavaScriptResolver.readJsonModel(jsFile);
            if (model == null) {
                throw new CompilerErrorException("Can't find metamodel definition in " + jsFile.getAbsolutePath());
            }
            if (!model.containsKey("$mod-bin")) {
                throw new CeylonRunJsException("The JavaScript module " + jsFile +
                        " is not compatible with the current version of ceylon-js");
            }
            return model;
        } catch (IOException ex) {
            throw new CompilerErrorException("Error loading model from " + jsFile);
        }
    }

    @Override
    public void resolveModule(final ArtifactResult artifact, final Module module,
            final ModuleImport moduleImport, LinkedList<Module> dependencyTree,
            List<PhasedUnits> phasedUnitsOfDependencies, boolean forCompiledModule) {
        if (!clLoaded) {
            clLoaded = true;
            //If we haven't loaded the language module yet, we need to load it first
            if (!(LANGUAGE_MODULE_NAME.equals(artifact.name())
                    && artifact.artifact().getName().endsWith(ArtifactContext.JS_MODEL))) {
                if (JsModuleManagerFactory.isVerbose()) {
                    System.out.println("Loading JS language module before any other modules");
                }
                ArtifactContext ac = new ArtifactContext(null, Module.LANGUAGE_MODULE_NAME,
                        module.getLanguageModule().getVersion(), ArtifactContext.JS_MODEL);
                ac.setIgnoreDependencies(true);
                ac.setThrowErrorIfMissing(true);
                ArtifactResult lmar = getContext().getRepositoryManager().getArtifactResult(ac);
                resolveModule(lmar, module.getLanguageModule(), null, dependencyTree,
                        phasedUnitsOfDependencies, forCompiledModule);
            }
            //Then we continue loading whatever they asked for first.
        }
        //Create a similar artifact but with -model.js extension
        File js = artifact.artifact();
        if (js.getName().endsWith(ArtifactContext.JS) && !js.getName().endsWith(ArtifactContext.JS_MODEL)) {
            ArtifactContext ac = new ArtifactContext(artifact.namespace(), artifact.name(),
                    artifact.version(), ArtifactContext.JS_MODEL);
            ac.setIgnoreDependencies(true);
            ac.setThrowErrorIfMissing(true);
            ArtifactResult lmar = getContext().getRepositoryManager().getArtifactResult(ac);
            js = lmar.artifact();
        }
        if (module instanceof JsonModule) {
            if (((JsonModule)module).getModel() != null) {
                return;
            }
            if (js.exists() && js.isFile() && js.canRead() && js.getName().endsWith(ArtifactContext.JS_MODEL)) {
                if (JsModuleManagerFactory.isVerbose()) {
                    System.out.println("Loading model from " + js);
                }
                Map<String,Object> model = loadJsonModel(js);
                if (model == null) {
                    if (JsModuleManagerFactory.isVerbose()) {
                        System.out.println("Model not found in " + js);
                    }
                } else {
                    loadModuleFromMap(artifact, module, dependencyTree, phasedUnitsOfDependencies,
                            forCompiledModule, model);
                    return;
                }
            }
            if ("npm".equals(artifact.namespace())) {
                try {
                    final File root = ((AbstractRepository)artifact.repository()).getRoot().getContent(File.class);
                    final String npmPath = artifact.artifact().getAbsolutePath().replace(File.separator, "/");
                    ((JsonModule)module).setNpmPath(npmPath.substring(root.getAbsolutePath().length()+1));
                    module.setJsMajor(Versions.JS_BINARY_MAJOR_VERSION);
                    module.setJsMinor(Versions.JS_BINARY_MINOR_VERSION);
                } catch (IOException ex) {
                    System.out.println("ay no mames");
                    ex.printStackTrace();
                }
                return;
            }
        }
        super.resolveModule(artifact, module, moduleImport, dependencyTree,
                phasedUnitsOfDependencies, forCompiledModule);
    }

    @Override
    protected PhasedUnits createPhasedUnits() {
        PhasedUnits units = super.createPhasedUnits();
        String fileEncoding = encoding;
        if (fileEncoding == null) {
            fileEncoding = CeylonConfig.get(DefaultToolOptions.DEFAULTS_ENCODING);
        }
        if (fileEncoding != null) {
            units.setEncoding(fileEncoding);
        }
        return units;
    }
}
