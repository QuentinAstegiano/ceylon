package com.redhat.ceylon.compiler.js;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.redhat.ceylon.common.Backend;
import com.redhat.ceylon.compiler.js.util.JsIdentifierNames;
import com.redhat.ceylon.compiler.typechecker.tree.Tree;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.Condition;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.ExistsCondition;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.ExistsOrNonemptyCondition;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.Expression;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.IsCase;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.IsCondition;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.MatchCase;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.NonemptyCondition;
import com.redhat.ceylon.model.typechecker.model.ClassOrInterface;
import com.redhat.ceylon.model.typechecker.model.ConditionScope;
import com.redhat.ceylon.model.typechecker.model.Declaration;
import com.redhat.ceylon.model.typechecker.model.ModelUtil;
import com.redhat.ceylon.model.typechecker.model.Value;

/** This component is used by the main JS visitor to generate code for conditions.
 * 
 * @author Enrique Zamudio
 * @author Ivo Kasiuk
 */
public class ConditionGenerator {

    private final GenerateJsVisitor gen;
    private final JsIdentifierNames names;
    private final Set<Declaration> directAccess;

    ConditionGenerator(GenerateJsVisitor owner, JsIdentifierNames names, Set<Declaration> directDeclarations) {
        gen = owner;
        this.names = names;
        directAccess = directDeclarations;
    }

    /** Generate a list of all the variables from conditions in the list.
     * @param conditions The ConditionList that may contain variable declarations.
     * @param output Whether to generate the variable declarations or not. */
    List<VarHolder> gatherVariables(Tree.ConditionList conditions, boolean output) {
        ArrayList<VarHolder> vars = new ArrayList<VarHolder>();
        boolean first = true;
        for (Condition cond : conditions.getConditions()) {
            Tree.Variable variable = null;
            Tree.Destructure destruct = null;
            if (cond instanceof ExistsOrNonemptyCondition) {
                if (((ExistsOrNonemptyCondition) cond).getVariable() instanceof Tree.Variable) {
                    variable = (Tree.Variable)((ExistsOrNonemptyCondition) cond).getVariable();
                } else if (((ExistsOrNonemptyCondition) cond).getVariable() instanceof Tree.Destructure) {
                    destruct = (Tree.Destructure)((ExistsOrNonemptyCondition) cond).getVariable();
                }
            } else if (cond instanceof IsCondition) {
                variable = ((IsCondition) cond).getVariable();
            } else if (!(cond instanceof Tree.BooleanCondition)) {
                cond.addUnexpectedError("No support for conditions of type " + cond.getClass().getSimpleName(), Backend.JavaScript);
                return null;
            }
            if (variable != null) {
                final Tree.Term variableRHS = variable.getSpecifierExpression().getExpression().getTerm();
                final Value vdecl = variable.getDeclarationModel();
                String varName = names.name(vdecl);
                final boolean member = ModelUtil.getRealScope(vdecl.getContainer()) instanceof ClassOrInterface;
                if (member) {
                    //Assertions and special conditions in an initializer block can declare variables
                    //that should become attributes. For some reason the typechecker doesn't do this.
                    if (vdecl.getScope() instanceof ConditionScope) {
                        vdecl.setContainer(ModelUtil.getRealScope(vdecl.getContainer()));
                        varName = names.name(vdecl);
                    }
                } else if (output) {
                    if (first) {
                        first = false;
                        gen.out("var ");
                    } else {
                        gen.out(",");
                    }
                    gen.out(varName);
                }
                vars.add(new VarHolder(variable, variableRHS, varName, member));
            } else if (destruct != null) {
                final Destructurer d=new Destructurer(destruct.getPattern(), null, directAccess, "", first);
                for (Tree.Variable v : d.getVariables()) {
                    if (output) {
                        final String vname = names.name(v.getDeclarationModel());
                        if (first) {
                            first = false;
                            gen.out("var ", vname);
                        } else {
                            gen.out(",", vname);
                        }
                    }
                }
                VarHolder vh = new VarHolder(destruct, null, null, false);
                vh.vars = d.getVariables();
                vars.add(vh);
            }
        }
        if (output && !first) {
            gen.endLine(true);
        }
        return vars;
    }

    /** Generates the declaration of all the variables that were gathered by
     * gartherVariables. Useful when they weren't generated by that method. */
    void outputVariables(List<VarHolder> vars) {
        boolean first=true;
        for (VarHolder vh : vars) {
            if (vh.name != null) {
                if (first) {
                    gen.out("var ", vh.name);
                    first=false;
                } else {
                    gen.out(",", vh.name);
                }
            } else if (vh.destr != null && vh.vars != null) {
                for (Tree.Variable v : vh.vars) {
                    final String vname = names.name(v.getDeclarationModel());
                    if (first) {
                        gen.out("var ", vname);
                        first=false;
                    } else {
                        gen.out(",", vname);
                    }
                }
            }
        }
        if (!first) {
            gen.endLine(true);
        }
    }

    /** Handles the "is", "exists" and "nonempty" conditions */
    List<VarHolder> specialConditionsAndBlock(Tree.ConditionList conditions,
            Tree.Block block, String keyword) {
        final List<VarHolder> vars = gatherVariables(conditions, true);
        specialConditions(vars, conditions, keyword);
        if (block != null) {
            gen.encloseBlockInFunction(block, true, getCaptured(vars));
        }
        return vars;
    }

    /** Handles the "is", "exists" and "nonempty" conditions, with a pre-generated
     * list of the variables from the conditions. */
    void specialConditions(final List<VarHolder> vars, Tree.ConditionList conditions, String keyword) {
        //The first pass is gathering the conditions, which we already get here
        //Second pass: generate the conditions
        gen.out(keyword);
        if (!keyword.isEmpty()) {
            gen.out("(");
        }
        boolean first = true;
        final Iterator<VarHolder> ivars = vars.iterator();
        for (Condition cond : conditions.getConditions()) {
            if (first) {
                first = false;
            } else {
                gen.out("&&");
            }
            if (cond instanceof Tree.BooleanCondition) {
                cond.visit(gen);
            } else {
                VarHolder vh = ivars.next();
                if (vh.destr == null) {
                    if (vh.member) {
                        String cname = gen.getNames().self(((ClassOrInterface)ModelUtil.getRealScope(
                                vh.var.getDeclarationModel().getContainer())));
                        specialConditionCheck(cond, vh.term, cname + "." + vh.name);
                    } else {
                        specialConditionCheck(cond, vh.term, vh.name);
                        directAccess.add(vh.var.getDeclarationModel());
                    }
                } else {
                    destructureCondition(cond, vh);
                }
            }
        }
        if (!keyword.isEmpty()) {
            gen.out(")");
        }
    }

    private void specialConditionCheck(Condition condition, Tree.Term variableRHS, String varName) {
        if (condition instanceof ExistsOrNonemptyCondition) {
            if (((ExistsOrNonemptyCondition) condition).getNot()) {
                gen.out("!");
            }
            if (condition instanceof Tree.NonemptyCondition) {
                gen.out(gen.getClAlias(), "ne$(");
            } else { //exists
                gen.out(gen.getClAlias(), "nn$(");
            }
            specialConditionRHS(variableRHS, varName);
            gen.out(")");
        } else {
            Tree.Type type = ((IsCondition) condition).getType();
            gen.generateIsOfType(variableRHS, null, type.getTypeModel(),
                    varName, ((IsCondition)condition).getNot());
        }
    }

    void specialConditionRHS(Tree.Term variableRHS, String varName) {
        if (varName == null) {
            if (variableRHS!=null) {
                variableRHS.visit(gen);
            }
        } else {
            gen.out("(", varName, "=");
            variableRHS.visit(gen);
            gen.out(")");
        }
    }

    void specialConditionRHS(String variableRHS, String varName) {
        if (varName == null) {
            gen.out(variableRHS);
        } else {
            gen.out("(", varName, "=");
            gen.out(variableRHS);
            gen.out(")");
        }
    }

    /** Generates JS code for an "if" statement. */
    void generateIf(Tree.IfStatement that) {
        final Tree.IfClause ifClause = that.getIfClause();
        final Tree.Block ifBlock = ifClause.getBlock();
        final Tree.ElseClause anoserque = that.getElseClause();
        final List<VarHolder> vars = specialConditionsAndBlock(ifClause.getConditionList(), ifBlock, "if");
        if (anoserque != null) {
            final Tree.Variable elsevar = anoserque.getVariable();
            final Value elseDec = elsevar != null ? elsevar.getDeclarationModel() : null;
            if (elsevar != null) {
                for (VarHolder vh : vars) {
                    if (vh.var.getDeclarationModel().getName().equals(elseDec.getName())) {
                        names.forceName(elseDec, vh.name);
                        directAccess.add(elseDec);
                        break;
                    }
                }
            }
            gen.out("else");
            gen.encloseBlockInFunction(anoserque.getBlock(), true,
                    elseDec != null && elseDec.isCaptured() ?
                            Collections.singleton(elseDec) : null);
            if (elseDec != null) {
                directAccess.remove(elseDec);
                names.forceName(elseDec, null);
            }
        }
        for (VarHolder v : vars) {
            v.forget();
        }
    }

    void generateIfExpression(Tree.IfExpression that, boolean nested) {
        final List<VarHolder> vars = gatherVariables(that.getIfClause().getConditionList(), false);
        final Tree.ElseClause anoserque = that.getElseClause();
        final Tree.Variable elsevar = anoserque == null ? null : anoserque.getVariable();
        if (elsevar != null) {
            final Value elseval = elsevar.getDeclarationModel();
            for (VarHolder vh : vars) {
                if (vh.var != null && vh.var.getDeclarationModel().getName().equals(elseval.getName())) {
                    names.forceName(elseval, vh.name);
                    directAccess.add(elseval);
                    break;
                }
            }
        }
        if (vars.isEmpty()) {
            if (nested) {
                gen.out(" return ");
            } else {
                gen.out("(");
            }
            //No special conditions means we can use a simple ternary
            specialConditions(vars, that.getIfClause().getConditionList(), "");
            gen.out("?");
            that.getIfClause().getExpression().visit(gen);
            gen.out(":");
            if (anoserque == null) {
                gen.out("null;");
            } else {
                anoserque.getExpression().visit(gen);
            }
            if (!nested) {
                gen.out(")");
            }
        } else {
            if (nested) {
                gen.out("{");
            } else {
                gen.out("function(){");
            }
            outputVariables(vars);
            specialConditions(vars, that.getIfClause().getConditionList(), "if");
            gen.out("return ");
            that.getIfClause().getExpression().visit(gen);
            for (VarHolder v : vars) {
                v.forget();
            }
            final boolean thenIf = anoserque != null &&
                    anoserque.getExpression().getTerm() instanceof Tree.IfExpression;
            if (thenIf) {
                gen.out(";else");
            } else {
                gen.out(";else return ");
            }
            if (anoserque == null) {
                gen.out("null;");
            } else if (thenIf) {
                generateIfExpression((Tree.IfExpression)anoserque.getExpression().getTerm(), true);
            } else {
                anoserque.getExpression().visit(gen);
            }
            if (nested) {
                gen.out("}");
            } else {
                gen.out("}()");
            }
        }
        if (elsevar != null) {
            directAccess.remove(elsevar.getDeclarationModel());
            names.forceName(elsevar.getDeclarationModel(), null);
        }
    }
    /** Generates JS code for a WhileStatement. */
    void generateWhile(Tree.WhileStatement that) {
        Tree.WhileClause whileClause = that.getWhileClause();
        List<VarHolder> vars = specialConditionsAndBlock(whileClause.getConditionList(),
                whileClause.getBlock(), "while");
        for (VarHolder v : vars) {
            v.forget();
        }
    }

    private void generateSwitch(final Tree.SwitchClause clause, final Tree.SwitchCaseList cases, final SwitchGen sgen) {
        final String expvar;
        final Expression expr;
        if (clause.getSwitched().getExpression() == null) {
            expvar = names.name(clause.getSwitched().getVariable().getDeclarationModel());
            expr = clause.getSwitched().getVariable().getSpecifierExpression().getExpression();
            directAccess.add(clause.getSwitched().getVariable().getDeclarationModel());
        } else {
            //Put the expression in a tmp var
            expr = clause.getSwitched().getExpression();
            expvar = names.createTempVariable();
        }
        sgen.gen1(expvar, expr);
        //For each case, do an if
        boolean first = true;
        for (Tree.CaseClause cc : cases.getCaseClauses()) {
            if (!first) gen.out("else ");
            caseClause(cc, expvar, expr.getTerm());
            first = false;
        }
        final Tree.ElseClause anoserque = cases.getElseClause();
        if (anoserque == null) {
            if (gen.isInDynamicBlock() && ModelUtil.isTypeUnknown(expr.getTypeModel())) {
                gen.out("else throw ", gen.getClAlias(), "Exception('Ceylon switch over unknown type does not cover all cases')");
            } else {
                gen.out("else throw ", gen.getClAlias(), "Exception('Supposedly exhaustive switch was not exhaustive')");
            }
        } else {
            final Tree.Variable elsevar = anoserque.getVariable();
            if (elsevar != null) {
                directAccess.add(elsevar.getDeclarationModel());
                names.forceName(elsevar.getDeclarationModel(), expvar);
            }
            sgen.gen2(anoserque);
            if (elsevar != null) {
                directAccess.remove(elsevar.getDeclarationModel());
                names.forceName(elsevar.getDeclarationModel(), null);
            }
        }
        sgen.gen3(expr);
        if (clause.getSwitched().getExpression() == null) {
            directAccess.remove(clause.getSwitched().getVariable().getDeclarationModel());
        }
    }

    void switchStatement(final Tree.SwitchStatement that) {
        generateSwitch(that.getSwitchClause(), that.getSwitchCaseList(), new SwitchGen() {
            public void gen1(String expvar, Expression expr) {
                gen.out("var ", expvar, "=");
                expr.visit(gen);
                gen.endLine(true);
            }
            public void gen2(Tree.ElseClause anoserque) {
                gen.out("else");
                anoserque.getBlock().visit(gen);
            }
            public void gen3(Expression expr) {}
        });
    }

    void switchExpression(final Tree.SwitchExpression that) {
        generateSwitch(that.getSwitchClause(), that.getSwitchCaseList(), new SwitchGen() {
            public void gen1(String expvar, Expression expr) {
                gen.out("function(", expvar, "){");
            }
            public void gen2(Tree.ElseClause anoserque) {
                gen.out("else return ");
                anoserque.getExpression().visit(gen);
            }
            public void gen3(Expression expr) {
                gen.out("}(");
                expr.visit(gen);
                gen.out(")");
            }
        });
    }

    /** Generates code for a case clause, as part of a switch statement. Each case
     * is rendered as an if. */
    private void caseClause(final Tree.CaseClause cc, String expvar, final Tree.Term switchTerm) {
        gen.out("if(");
        final Tree.CaseItem item = cc.getCaseItem();
        Tree.Variable caseVar = null;
        Value caseDec = null;
        if (item instanceof IsCase) {
            IsCase isCaseItem = (IsCase) item;
            gen.generateIsOfType(item, expvar, isCaseItem.getType().getTypeModel(), null, false);
            caseVar = isCaseItem.getVariable();
            if (caseVar != null) {
                caseDec = caseVar.getDeclarationModel();
                directAccess.add(caseDec);
                names.forceName(caseDec, expvar);
            }
        } else if (item instanceof Tree.SatisfiesCase) {
            item.addError("case(satisfies) not yet supported", Backend.JavaScript);
            gen.out("true");
        } else if (item instanceof MatchCase) {
            final boolean isNull = switchTerm.getTypeModel().covers(switchTerm.getUnit().getNullType());
            boolean first = true;
            for (Expression exp : ((MatchCase)item).getExpressionList().getExpressions()) {
                if (!first) gen.out(" || ");
                final Tree.Term term = exp.getTerm();
                if (term instanceof Tree.StringLiteral || term instanceof Tree.NaturalLiteral) {
                    if (isNull) {
                        gen.out(gen.getClAlias(), "nn$(", expvar, ")&&");
                    }
                    exp.visit(gen);
                    gen.out(".equals(", expvar, ")");
                } else if (ModelUtil.isTypeUnknown(switchTerm.getTypeModel())) {
                    gen.out(expvar, "===");
                    if (!gen.isNaturalLiteral(term)) {
                        exp.visit(gen);
                    }
                } else if (term instanceof Tree.Literal) {
                    if (switchTerm.getUnit().isOptionalType(switchTerm.getTypeModel())) {
                        gen.out(expvar, "!==null&&");
                    }
                    gen.out(expvar, ".equals(");
                    exp.visit(gen);
                    gen.out(")");
                } else if (term instanceof Tree.Tuple) {
                    if (((Tree.Tuple) term).getSequencedArgument() == null) {
                        gen.out(expvar, "===", gen.getClAlias(), "empty()");
                    } else {
                        gen.out(gen.getClAlias(), "is$(", expvar, ",{t:",gen.getClAlias(), "Tuple})&&",
                                expvar, ".equals(");
                        exp.visit(gen);
                        gen.out(")");
                    }
                } else {
                    gen.out(expvar, "===");
                    exp.visit(gen);
                }
                first = false;
            }
        } else {
            cc.addUnexpectedError("support for case of type " + cc.getClass().getSimpleName() + " not yet implemented", Backend.JavaScript);
        }
        gen.out(")");
        if (cc.getBlock() == null) {
            gen.out("return ");
            cc.getExpression().visit(gen);
            gen.out(";");
        } else {
            gen.out(" ");
            Set<Value> cap = caseDec != null && caseDec.isCaptured() ?
                    Collections.singleton(caseDec) : null;
            gen.encloseBlockInFunction(cc.getBlock(), true, cap);
        }
        if (caseDec != null) {
            directAccess.remove(caseDec);
            names.forceName(caseDec, null);
        }
    }

    void destructureCondition(Condition cond, VarHolder vh) {
        final String expvar = names.createTempVariable();
        gen.out("function(", expvar, "){if(");
        if (cond instanceof ExistsCondition) {
            if (!((ExistsCondition)cond).getNot()) {
                gen.out("!");
            }
            gen.out(gen.getClAlias(), "nn$(", expvar, "))return false;");
        } else if (cond instanceof NonemptyCondition) {
            if (!((NonemptyCondition)cond).getNot()) {
                gen.out("!");
            }
            gen.out(gen.getClAlias(), "ne$(", expvar, "))return false;");
        } else {
            Tree.Type type = ((IsCondition) cond).getType();
            gen.generateIsOfType(null, expvar, type.getTypeModel(),
                    null, ((IsCondition)cond).getNot());
            gen.out(")return false;");
        }
        gen.out("return(");
        final Destructurer d=new Destructurer(vh.destr.getPattern(), gen, directAccess, expvar, true);
        gen.out(",true);}(");
        vh.destr.getSpecifierExpression().visit(gen);
        gen.out(")");
        vh.vars=d.getVariables();
        vh.captured=d.getCapturedValues();
    }

    /** Get all the captured variables from the set of VarHolders. */
    Set<Value> getCaptured(List<VarHolder> vars) {
        Set<Value> caps = new HashSet<>(3);
        for (VarHolder vh : vars) {
            Set<Value> c2 = vh.getCaptured();
            if (c2 != null && !c2.isEmpty()) {
                caps.addAll(c2);
            }
        }
        return caps;
    }

    /** Holder for a special condition's variable, its right-hand side term,
     * and its name in the generated js. */
    class VarHolder {
        final Tree.Variable var;
        final Tree.Term term;
        final String name;
        final Tree.Destructure destr;
        Set<Tree.Variable> vars;
        Set<Value> captured;
        final boolean member;
        private VarHolder(Tree.Statement st, Tree.Term rhs, String varName, boolean member) {
            if (st instanceof Tree.Variable) {
                var = (Tree.Variable)st;
                destr = null;
                if (var.getDeclarationModel() != null && var.getDeclarationModel().isCaptured()) {
                    captured = Collections.singleton(var.getDeclarationModel());
                }
            } else {
                var = null;
                destr = (Tree.Destructure)st;
            }
            term = rhs;
            if (st.getScope() instanceof Tree.Assertion &&
                    st.getScope().getContainer() instanceof ClassOrInterface) {
                name = gen.getNames().self((ClassOrInterface)st.getScope().getContainer()) + "." + varName;
                if (var != null) {
                    names.forceName(var.getDeclarationModel(), name);
                }
            } else {
                name = varName;
            }
            this.member = member;
        }
        void forget() {
            if (var != null) {
                directAccess.remove(var.getDeclarationModel());
                names.forceName(var.getDeclarationModel(), null);
            } else if (vars != null) {
                for (Tree.Variable v : vars) {
                    directAccess.remove(v.getDeclarationModel());
                    names.forceName(v.getDeclarationModel(), null);
                }
            }
        }
        public Set<Value> getCaptured() {
            return captured;
        }
        public String toString() {
            if (var != null) {
                return "VarHolder(" + var.getDeclarationModel() + ")";
            } else if (vars != null) {
                final StringBuilder sb = new StringBuilder();
                for (Tree.Variable v : vars) {
                    if (sb.length() == 0) {
                        sb.append("VarHolder[");
                    } else {
                        sb.append(",");
                    }
                    sb.append(v.getDeclarationModel());
                }
                sb.append("]");
                return sb.toString();
            }
            return "VarHolder[???]";
        }
    }

    private interface SwitchGen {
        void gen1(String expvar, Expression expr);
        void gen2(Tree.ElseClause anoserque);
        void gen3(Expression expr);
    }
}
