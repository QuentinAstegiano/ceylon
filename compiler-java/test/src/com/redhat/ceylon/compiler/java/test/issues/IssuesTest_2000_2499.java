/*
 * Copyright Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the authors tag. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License version 2.
 * 
 * This particular file is subject to the "Classpath" exception as provided in the 
 * LICENSE file that accompanied this code.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package com.redhat.ceylon.compiler.java.test.issues;

import org.junit.Test;

import com.redhat.ceylon.compiler.java.test.CompilerTest;


public class IssuesTest_2000_2499 extends CompilerTest {

    @Override
    protected ModuleWithArtifact getDestModuleWithArtifact(String main){
        return new ModuleWithArtifact("com.redhat.ceylon.compiler.java.test.issues", "1");
    }
    
    @Override
    protected String transformDestDir(String name) {
        return name + "-2000-2499";
    }

    @Test
    public void testBug2001() {
        compareWithJavaSource("bug20xx/Bug2001");
    }

    @Test
    public void testBug2003() {
        compile("bug20xx/bug2003/Bug2003.java");
    }

    @Test
    public void testBug2005() {
        compareWithJavaSource("bug20xx/Bug2005");
    }
    
    @Test
    public void testBug2007() {
        compareWithJavaSource("bug20xx/Bug2007");
        run("com.redhat.ceylon.compiler.java.test.issues.bug20xx.Bug2007_2");
    }

    @Test
    public void testBug2011() {
        compareWithJavaSource("bug20xx/bug2011/Bug2011");
    }
    
    @Test
    public void testBug2012() {
        compareWithJavaSource("bug20xx/Bug2012");
    }
    
    @Test
    public void testBug2013() {
        compareWithJavaSource("bug20xx/Bug2013");
    }

    @Test
    public void testBug2016() {
        compareWithJavaSource("bug20xx/Bug2016");
    }

    @Test
    public void testBug2021() {
        compareWithJavaSource("bug20xx/Bug2021");
    }
    
    @Test
    public void testBug2024() {
        compile("bug20xx/bug2024a/module.ceylon", "bug20xx/bug2024a/package.ceylon", "bug20xx/bug2024a/Instant.ceylon");
        compile("bug20xx/bug2024b/module.ceylon", "bug20xx/bug2024b/Bug2024.ceylon", "bug20xx/bug2024b/Bug2024.java");
    }

    @Test
    public void testBug2034() {
        compile("bug20xx/Bug2034.ceylon");
        run("com.redhat.ceylon.compiler.java.test.issues.bug20xx.bug2034");
    }
}
