/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package com.redhat.ceylon.cmr.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;

/**
 * I/O utils.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class IOUtils {

    static InputStream toInputStream(File file) {
        try {
            return new FileInputStream(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static <T extends Serializable> InputStream toObjectStream(T content) {
        if (content == null)
            throw new IllegalArgumentException("Null content");
        return null;  // TODO
    }

    static <T> T fromStream(Class<T> contentType, InputStream inputStream) {
        if (inputStream == null)
            throw new IllegalArgumentException("Null input stream!");

        return null;  // TODO
    }
}
