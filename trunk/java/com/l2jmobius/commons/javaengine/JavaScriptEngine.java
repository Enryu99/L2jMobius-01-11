/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. 
 * Use is subject to license terms.
 *
 * Redistribution and use in source and binary forms, with or without modification, are 
 * permitted provided that the following conditions are met: Redistributions of source code 
 * must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of 
 * conditions and the following disclaimer in the documentation and/or other materials 
 * provided with the distribution. Neither the name of the Sun Microsystems nor the names of 
 * is contributors may be used to endorse or promote products derived from this software 
 * without specific prior written permission. 

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY 
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER 
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.l2jmobius.commons.javaengine;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

/**
 * This is script engine for Java programming language.
 * @author A. Sundararajan
 */
public class JavaScriptEngine extends AbstractScriptEngine implements Compilable
{
	// Java compiler
	private final JavaCompiler compiler;
	
	public JavaScriptEngine()
	{
		compiler = new JavaCompiler();
	}
	
	// my factory, may be null
	private ScriptEngineFactory factory;
	
	// my implementation for CompiledScript
	private static class JavaCompiledScript extends CompiledScript implements Serializable
	{
		private final transient JavaScriptEngine _engine;
		private transient Class<?> _class;
		private final Map<String, byte[]> _classBytes;
		private final String _classPath;
		
		JavaCompiledScript(JavaScriptEngine engine, Map<String, byte[]> classBytes, String classPath)
		{
			_engine = engine;
			_classBytes = classBytes;
			_classPath = classPath;
		}
		
		@Override
		public ScriptEngine getEngine()
		{
			return _engine;
		}
		
		@Override
		public Object eval(ScriptContext ctx) throws ScriptException
		{
			if (_class == null)
			{
				final Map<String, byte[]> classBytesCopy = new HashMap<>();
				classBytesCopy.putAll(_classBytes);
				final MemoryClassLoader loader = new MemoryClassLoader(classBytesCopy, _classPath, JavaScriptEngine.getParentLoader(ctx));
				_class = JavaScriptEngine.parseMain(loader, ctx);
			}
			return JavaScriptEngine.evalClass(_class, ctx);
		}
	}
	
	@Override
	public CompiledScript compile(String script) throws ScriptException
	{
		return compile(script, context);
	}
	
	@Override
	public CompiledScript compile(Reader reader) throws ScriptException
	{
		return compile(readFully(reader));
	}
	
	@Override
	public Object eval(String str, ScriptContext ctx) throws ScriptException
	{
		final Class<?> clazz = parse(str, ctx);
		return evalClass(clazz, ctx);
	}
	
	@Override
	public Object eval(Reader reader, ScriptContext ctx) throws ScriptException
	{
		return eval(readFully(reader), ctx);
	}
	
	@Override
	public ScriptEngineFactory getFactory()
	{
		synchronized (this)
		{
			if (factory == null)
			{
				factory = new JavaScriptEngineFactory();
			}
		}
		return factory;
	}
	
	@Override
	public Bindings createBindings()
	{
		return new SimpleBindings();
	}
	
	void setFactory(ScriptEngineFactory factory)
	{
		this.factory = factory;
	}
	
	// Internals only below this point
	
	private Class<?> parse(String str, ScriptContext ctx) throws ScriptException
	{
		final String fileName = getFileName(ctx);
		final String sourcePath = getSourcePath(ctx);
		final String classPath = getClassPath(ctx);
		
		Writer err = ctx.getErrorWriter();
		if (err == null)
		{
			err = new StringWriter();
		}
		
		final Map<String, byte[]> classBytes = compiler.compile(fileName, str, err, sourcePath, classPath);
		
		if (classBytes == null)
		{
			if (err instanceof StringWriter)
			{
				throw new ScriptException(((StringWriter) err).toString());
			}
			throw new ScriptException("compilation failed");
		}
		
		// create a ClassLoader to load classes from MemoryJavaFileManager
		final MemoryClassLoader loader = new MemoryClassLoader(classBytes, classPath, getParentLoader(ctx));
		return parseMain(loader, ctx);
	}
	
	protected static Class<?> parseMain(MemoryClassLoader loader, ScriptContext ctx) throws ScriptException
	{
		final String mainClassName = getMainClassName(ctx);
		if (mainClassName != null)
		{
			try
			{
				final Class<?> clazz = loader.load(mainClassName);
				final Method mainMethod = findMainMethod(clazz);
				if (mainMethod == null)
				{
					throw new ScriptException("no main method in " + mainClassName);
				}
				return clazz;
			}
			catch (ClassNotFoundException cnfe)
			{
				cnfe.printStackTrace();
				throw new ScriptException(cnfe);
			}
		}
		
		// no main class configured - load all compiled classes
		Iterable<Class<?>> classes;
		try
		{
			classes = loader.loadAll();
		}
		catch (ClassNotFoundException exp)
		{
			throw new ScriptException(exp);
		}
		
		// search for class with main method
		final Class<?> c = findMainClass(classes);
		if (c != null)
		{
			return c;
		}
		
		// if class with "main" method, then
		// return first class
		final Iterator<Class<?>> itr = classes.iterator();
		if (itr.hasNext())
		{
			return itr.next();
		}
		return null;
	}
	
	private JavaCompiledScript compile(String str, ScriptContext ctx) throws ScriptException
	{
		final String fileName = getFileName(ctx);
		final String sourcePath = getSourcePath(ctx);
		final String classPath = getClassPath(ctx);
		
		Writer err = ctx.getErrorWriter();
		if (err == null)
		{
			err = new StringWriter();
		}
		
		final Map<String, byte[]> classBytes = compiler.compile(fileName, str, err, sourcePath, classPath);
		if (classBytes == null)
		{
			if (err instanceof StringWriter)
			{
				throw new ScriptException(((StringWriter) err).toString());
			}
			throw new ScriptException("compilation failed");
		}
		
		return new JavaCompiledScript(this, classBytes, classPath);
	}
	
	private static Class<?> findMainClass(Iterable<Class<?>> classes)
	{
		// find a public class with public static main method
		for (Class<?> clazz : classes)
		{
			final int modifiers = clazz.getModifiers();
			if (Modifier.isPublic(modifiers))
			{
				final Method mainMethod = findMainMethod(clazz);
				if (mainMethod != null)
				{
					return clazz;
				}
			}
		}
		
		// okay, try to find package private class that
		// has public static main method
		for (Class<?> clazz : classes)
		{
			final Method mainMethod = findMainMethod(clazz);
			if (mainMethod != null)
			{
				return clazz;
			}
		}
		
		// no main class found!
		return null;
	}
	
	// find public static void main(String[]) method, if any
	private static Method findMainMethod(Class<?> clazz)
	{
		try
		{
			final Method mainMethod = clazz.getMethod("main", new Class[]
			{
				String[].class
			});
			final int modifiers = mainMethod.getModifiers();
			if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers))
			{
				return mainMethod;
			}
		}
		catch (NoSuchMethodException nsme)
		{
		}
		return null;
	}
	
	// find public static void setScriptContext(ScriptContext) method, if any
	private static Method findSetScriptContextMethod(Class<?> clazz)
	{
		try
		{
			final Method setCtxMethod = clazz.getMethod("setScriptContext", new Class[]
			{
				ScriptContext.class
			});
			final int modifiers = setCtxMethod.getModifiers();
			if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers))
			{
				return setCtxMethod;
			}
		}
		catch (NoSuchMethodException nsme)
		{
		}
		return null;
	}
	
	private static String getFileName(ScriptContext ctx)
	{
		final int scope = ctx.getAttributesScope("javax.script.filename");
		if (scope != -1)
		{
			return ctx.getAttribute("javax.script.filename", scope).toString();
		}
		return "$unnamed.java";
	}
	
	// for certain variables, we look for System properties. This is
	// the prefix used for such System properties
	private static final String SYSPROP_PREFIX = "com.sun.script.java.";
	
	private static final String[] EMPTY_STRING_ARRAY = new String[0];
	private static final String ARGUMENTS = "arguments";
	
	private static String[] getArguments(ScriptContext ctx)
	{
		final int scope = ctx.getAttributesScope(ARGUMENTS);
		if (scope != -1)
		{
			final Object obj = ctx.getAttribute(ARGUMENTS, scope);
			if (obj instanceof String[])
			{
				return (String[]) obj;
			}
		}
		// return zero length array
		return EMPTY_STRING_ARRAY;
	}
	
	private static final String SOURCEPATH = "sourcepath";
	
	private static String getSourcePath(ScriptContext ctx)
	{
		final int scope = ctx.getAttributesScope(SOURCEPATH);
		if (scope != -1)
		{
			return ctx.getAttribute(SOURCEPATH).toString();
		}
		
		// look for "com.sun.script.java.sourcepath"
		return System.getProperty(SYSPROP_PREFIX + SOURCEPATH);
	}
	
	private static final String CLASSPATH = "classpath";
	
	private static String getClassPath(ScriptContext ctx)
	{
		final int scope = ctx.getAttributesScope(CLASSPATH);
		if (scope != -1)
		{
			return ctx.getAttribute(CLASSPATH).toString();
		}
		
		// look for "com.sun.script.java.classpath"
		String res = System.getProperty(SYSPROP_PREFIX + CLASSPATH);
		if (res == null)
		{
			res = System.getProperty("java.class.path");
		}
		return res;
	}
	
	private static final String MAINCLASS = "mainClass";
	
	private static String getMainClassName(ScriptContext ctx)
	{
		final int scope = ctx.getAttributesScope(MAINCLASS);
		if (scope != -1)
		{
			return ctx.getAttribute(MAINCLASS).toString();
		}
		
		// look for "com.sun.script.java.mainClass"
		return System.getProperty("com.sun.script.java.mainClass");
	}
	
	private static final String PARENTLOADER = "parentLoader";
	
	protected static ClassLoader getParentLoader(ScriptContext ctx)
	{
		final int scope = ctx.getAttributesScope(PARENTLOADER);
		if (scope != -1)
		{
			final Object loader = ctx.getAttribute(PARENTLOADER);
			if (loader instanceof ClassLoader)
			{
				return (ClassLoader) loader;
			}
		}
		return ClassLoader.getSystemClassLoader();
	}
	
	protected static Object evalClass(Class<?> clazz, ScriptContext ctx) throws ScriptException
	{
		// JSR-223 requirement
		ctx.setAttribute("context", ctx, 100);
		if (clazz == null)
		{
			return null;
		}
		try
		{
			final boolean isPublicClazz = Modifier.isPublic(clazz.getModifiers());
			
			// find the setScriptContext method
			final Method setCtxMethod = findSetScriptContextMethod(clazz);
			// call setScriptContext and pass current ctx variable
			if (setCtxMethod != null)
			{
				if (!isPublicClazz)
				{
					// try to relax access
					setCtxMethod.setAccessible(true);
				}
				setCtxMethod.invoke(null, new Object[]
				{
					ctx
				});
			}
			
			// find the main method
			final Method mainMethod = findMainMethod(clazz);
			if (mainMethod != null)
			{
				if (!isPublicClazz)
				{
					// try to relax access
					mainMethod.setAccessible(true);
				}
				
				// get "command line" args for the main method
				final String args[] = getArguments(ctx);
				
				// call main method
				mainMethod.invoke(null, new Object[]
				{
					args
				});
			}
			
			// return main class as eval's result
			return clazz;
		}
		catch (Exception exp)
		{
			exp.printStackTrace();
			throw new ScriptException(exp);
		}
	}
	
	// read a Reader fully and return the content as string
	private String readFully(Reader reader) throws ScriptException
	{
		final char[] arr = new char[8 * 1024]; // 8K at a time
		final StringBuilder buf = new StringBuilder();
		int numChars;
		try
		{
			while ((numChars = reader.read(arr, 0, arr.length)) > 0)
			{
				buf.append(arr, 0, numChars);
			}
		}
		catch (IOException exp)
		{
			throw new ScriptException(exp);
		}
		return buf.toString();
	}
	
}
