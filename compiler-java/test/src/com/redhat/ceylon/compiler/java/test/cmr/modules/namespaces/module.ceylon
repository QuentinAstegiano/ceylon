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
"Bla bla."
by("The Ceylon Team")
license("http://www.gnu.org/licenses/gpl.html")
native("jvm")
module com.redhat.ceylon.compiler.java.test.cmr.modules.namespaces "1.0.0" {
    import ceylon:"ceylon.collection" "1.2.0";
    import jdk:"java.base" "7";
    import maven:"org.apache.logging.log4j:log4j-core" "2.5";
    import "org.apache.logging.log4j:log4j-api" "2.5";
}