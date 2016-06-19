/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package leap.core.aop;

import leap.core.AppConfig;
import leap.core.annotation.Inject;
import leap.core.aop.config.MethodInterceptorConfig;
import leap.core.aop.interception.MethodInterception;
import leap.core.aop.interception.MethodInterceptor;
import leap.core.aop.interception.SimpleMethodInterception;
import leap.core.aop.matcher.AsmMethodInfo;
import leap.core.instrument.AppInstrumentClass;
import leap.core.instrument.AppInstrumentContext;
import leap.core.instrument.AsmInstrumentProcessor;
import leap.lang.Arrays2;
import leap.lang.Strings;
import leap.lang.Try;
import leap.lang.asm.*;
import leap.lang.asm.commons.GeneratorAdapter;
import leap.lang.asm.commons.Method;
import leap.lang.asm.tree.ClassNode;
import leap.lang.asm.tree.MethodNode;

import java.io.InputStream;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;
import java.util.function.Supplier;

import static leap.lang.asm.Opcodes.*;

public class AopInstrumentation extends AsmInstrumentProcessor {

    private static final Type PROVIDER_TYPE            = Type.getType(AopProvider.class);
    private static final Type INTERCEPTOR_TYPE         = Type.getType(MethodInterceptor.class);
    private static final Type INJECT_TYPE              = Type.getType(Inject.class);
    private static final Type SIMPLE_INTERCEPTION_TYPE = Type.getType(SimpleMethodInterception.class);
    private static final Type RUNNABLE_TYPE            = Type.getType(Runnable.class);
    private static final Type SUPPLIER_TYPE            = Type.getType(Supplier.class);
    private static final Type LAMBDA_FACTORY_TYPE      = Type.getType(LambdaMetafactory.class);

    private static final String PROVIDER_FIELD           = "$$aopProvider";
    private static final String INTERCEPTOR_FIELD_PREFIX = "$$aopInterceptor$";

    private static final Method METHOD_METADATA_FACTORY;
    private static final Method METHOD_PROVIDER_RUN1;
    private static final Method METHOD_PROVIDER_RUN2;
    private static final Method INTERCEPTION_CONSTRUCTOR1;
    private static final Method INTERCEPTION_CONSTRUCTOR2;
    private static final Method INTERCEPTION_CONSTRUCTOR3;
    private static final Method INTERCEPTION_CONSTRUCTOR4;

    static {
        try {
            java.lang.reflect.Method metadataFactory =
                    LambdaMetafactory.class
                        .getMethod("metafactory",
                                MethodHandles.Lookup.class,
                                String.class,
                                MethodType.class,
                                MethodType.class,
                                MethodHandle.class,
                                MethodType.class);

            METHOD_METADATA_FACTORY = Method.getMethod(metadataFactory);

            java.lang.reflect.Constructor c1 =
                    SimpleMethodInterception.class
                        .getConstructor(String.class, String.class, String. class,
                                        Object.class, MethodInterceptor[].class, Runnable.class);

            java.lang.reflect.Constructor c2 =
                    SimpleMethodInterception.class
                            .getConstructor(String.class, String.class, String. class,
                                    Object.class, Object[].class, MethodInterceptor[].class, Runnable.class);

            java.lang.reflect.Constructor c3 =
                    SimpleMethodInterception.class
                            .getConstructor(String.class, String.class, String. class,
                                    Object.class, MethodInterceptor[].class, Supplier.class);

            java.lang.reflect.Constructor c4 =
                    SimpleMethodInterception.class
                            .getConstructor(String.class, String.class, String. class,
                                    Object.class, Object[].class, MethodInterceptor[].class, Supplier.class);


            INTERCEPTION_CONSTRUCTOR1 = Method.getMethod(c1);
            INTERCEPTION_CONSTRUCTOR2 = Method.getMethod(c2);
            INTERCEPTION_CONSTRUCTOR3 = Method.getMethod(c3);
            INTERCEPTION_CONSTRUCTOR4 = Method.getMethod(c4);

            java.lang.reflect.Method run1 =
                    AopProvider.class
                            .getMethod("run", MethodInterception.class);

            java.lang.reflect.Method run2 =
                    AopProvider.class
                            .getMethod("runWithResult", MethodInterception.class);

            METHOD_PROVIDER_RUN1 = Method.getMethod(run1);
            METHOD_PROVIDER_RUN2 = Method.getMethod(run2);

        }catch(NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    private AopConfig config;

    @Override
    public void init(AppConfig config) {
        this.config = config.getExtension(AopConfig.class);
    }

    @Override
    protected boolean preInstrument(AppInstrumentContext context) {
        if(null == config || !config.isEnabled()) {
            return false;
        }
        return true;
    }

    @Override
    protected void processClass(AppInstrumentContext context, AppInstrumentClass ic, ClassInfo ci) {
        ClassNode cn = ci.cn;

        boolean isIgnore = isFrameworkClass(ci);
        if (isIgnore) {
            return;
        }

        List<AopMethod> methods = new ArrayList<>();

        for (MethodNode mn : cn.methods) {

            //static method not supported.
            if(ASM.isStatic(mn)) {
                continue;
            }

            AopMethod method = new AopMethod(cn, mn);

            List<MethodInterceptorConfig> interceptors = config.getMethodInterceptors(method);
            if(null != interceptors) {
                method.interceptors = interceptors;
                methods.add(method);
            }

        }

        if (methods.isEmpty()) {
            log.trace("Ignore class '{}', can't found any intercepted method(s).", ic.getClassName());
            return;
        } else {
            Try.throwUnchecked(() -> {
                try (InputStream in = ci.is.getInputStream()) {

                    byte[] bytes = instrumentClass(cn, new ClassReader(in), methods);

                    ASM.printASMifiedCode(bytes);

                    context.updateInstrumented(ic, this.getClass(), bytes, true);
                }
            });
        }
    }

    protected byte[] instrumentClass(ClassNode cn, ClassReader cr, List<AopMethod> methods) {
        AopClassVisitor visitor = new AopClassVisitor(cn ,new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES), methods);

        cr.accept(visitor, ClassReader.EXPAND_FRAMES);

        byte[] data = visitor.getClassData();

        return data;
    }

    protected static class AopClassVisitor extends ClassVisitor {

        private final ClassNode       cn;
        private final ClassWriter     cw;
        private final Type            type;
        private final List<AopMethod> methods;

        private final Map<String, String> nameInterceptorFields  = new HashMap<>();
        private final Map<String, String> classInterceptorFields = new HashMap<>();

        public AopClassVisitor(ClassNode cn, ClassWriter cw, List<AopMethod> methods) {
            super(ASM.API, cw);
            this.cn = cn;
            this.cw = cw;
            this.type = Type.getObjectType(cn.name);
            this.methods = methods;

            initInterceptorFields();
        }

        private void initInterceptorFields() {

            int count = 1;
            for(AopMethod am : methods) {
                for(MethodInterceptorConfig interceptor : am.interceptors) {
                    if(!Strings.isEmpty(interceptor.getBeanName())) {
                        if(!nameInterceptorFields.containsKey(interceptor.getBeanName())) {
                            nameInterceptorFields.put(interceptor.getBeanName(), INTERCEPTOR_FIELD_PREFIX + count);
                            count++;
                        }
                    }else{
                        if(!classInterceptorFields.containsKey(interceptor.getClassName())) {
                            classInterceptorFields.put(interceptor.getClassName(), INTERCEPTOR_FIELD_PREFIX + count);
                            count++;
                        }
                    }
                }
            }

        }

        private String getFieldName(MethodInterceptorConfig interceptor) {
            if(!Strings.isEmpty(interceptor.getBeanName())) {
                return nameInterceptorFields.get(interceptor.getBeanName());
            }else{
                return classInterceptorFields.get(interceptor.getClassName());
            }
        }

        private Type getFieldType(MethodInterceptorConfig interceptor) {
            if(!Strings.isEmpty(interceptor.getBeanName())) {
                return INTERCEPTOR_TYPE;
            }else{
                return Type.getType(ASM.getObjectTypeDescriptor(interceptor.getClassName()));
            }
        }

        private AopMethod isIntercepted(MethodNode m) {
            for(AopMethod am : methods) {
                if(am.getMethod() == m) {
                    return am;
                }
            }
            return null;
        }

        private List<MethodInterceptorConfig> getInterceptors(MethodNode m) {
            for(AopMethod am : methods) {
                if(am.getMethod() == m) {
                    return am.interceptors;
                }
            }
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodNode mn = ASM.getMethod(cn, name, desc);

            AopMethod am = isIntercepted(mn);

            if(null != am) {
                String newName = name + "$aop";

                visitInterceptedMethod(am, newName);
                visitLambdaMethod(am, newName);

                return super.visitMethod(access, newName, desc, signature, exceptions);
            }else{
                return super.visitMethod(access, name, desc, signature, exceptions);
            }
        }

        private static final Type   LOOKUP_TYPE  = Type.getType(MethodHandles.Lookup.class);
        private static final String LOOKUP_NAME  = "Lookup";
        private static final Type   HANDLES_TYPE = Type.getType(MethodHandles.class);

        private boolean lambadaInnerClassVisited;

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            if(name.equals(LOOKUP_TYPE.getInternalName()) &&
                    outerName.equals(HANDLES_TYPE.getInternalName()) &&
                    LOOKUP_NAME.equals(innerName)) {

                 lambadaInnerClassVisited = true;

            }
            super.visitInnerClass(name, outerName, innerName, access);
        }

        @Override
        public void visitEnd() {

            if(!lambadaInnerClassVisited) {
                cw.visitInnerClass(LOOKUP_TYPE.getInternalName(), HANDLES_TYPE.getInternalName(), LOOKUP_NAME, ACC_PUBLIC + ACC_FINAL + ACC_STATIC);
            }

            //visit fields
            FieldVisitor fv = cw.visitField(Opcodes.ACC_PRIVATE, PROVIDER_FIELD, PROVIDER_TYPE.getDescriptor(), null, null);
            {
                AnnotationVisitor av = fv.visitAnnotation(INJECT_TYPE.getDescriptor(), true);
                av.visitEnd();
            }

            for(Map.Entry<String,String> field : classInterceptorFields.entrySet()) {

                fv = cw.visitField(Opcodes.ACC_PRIVATE, field.getValue(), ASM.getObjectTypeDescriptor(field.getKey()), null, null);
                {
                    AnnotationVisitor av = fv.visitAnnotation(INJECT_TYPE.getDescriptor(), true);
                    av.visitEnd();
                }
            }

            for(Map.Entry<String,String> field : nameInterceptorFields.entrySet()) {

                fv = cw.visitField(Opcodes.ACC_PRIVATE, field.getValue(), INTERCEPTOR_TYPE.getDescriptor(), null, null);
                {
                    AnnotationVisitor av = fv.visitAnnotation(INJECT_TYPE.getDescriptor(), true);
                    av.visit("name", field.getKey());
                    av.visitEnd();
                }
            }

            fv.visitEnd();

            super.visitEnd();
        }

        public byte[] getClassData() {
            return cw.toByteArray();
        }

        protected void visitInterceptedMethod(AopMethod am, String newName) {
            MethodNode m = am.getMethod();

            MethodVisitor real = cw.visitMethod(m.access, m.name, m.desc, null,
                        m.exceptions == null ? null : m.exceptions.toArray(Arrays2.EMPTY_STRING_ARRAY));

            GeneratorAdapter mv = new GeneratorAdapter(real, m.access, m.name, m.desc);

            mv.visitCode();

            List<MethodInterceptorConfig> interceptors = am.interceptors;
            Type[] argumentTypes = ASM.getArgumentTypes(m);

            boolean hasReturnValue = ASM.hasReturnValue(m);

            mv.visitTypeInsn(NEW, SIMPLE_INTERCEPTION_TYPE.getInternalName());
            mv.visitInsn(DUP);
            mv.visitLdcInsn("cls");  //className
            mv.visitLdcInsn("name"); //methodName
            mv.visitLdcInsn("desc"); //methodDesc

            //object
            mv.loadThis();

            //args
            if(argumentTypes.length > 0) {
                mv.loadArgArray();
            }

            //interceptors
            mv.push(interceptors.size());
            mv.newArray(INTERCEPTOR_TYPE);
            for(int i=0;i<interceptors.size();i++) {
                MethodInterceptorConfig interceptor = interceptors.get(i);

                mv.dup();
                mv.push(i);
                mv.loadThis();
                mv.getField(type, getFieldName(interceptor), getFieldType(interceptor));
                mv.arrayStore(INTERCEPTOR_TYPE);
            }

            //runnable with lambada
            mv.loadThis();
            if(argumentTypes.length > 0) {
                mv.loadArgs();
            }


            Handle bsm = new Handle(Opcodes.H_INVOKESTATIC,
                                    LAMBDA_FACTORY_TYPE.getInternalName(),
                                    METHOD_METADATA_FACTORY.getName(),
                                    METHOD_METADATA_FACTORY.getDescriptor());

            Object[] bsmArgs;

            if(hasReturnValue) {
                final String desc = "(" + type.getDescriptor() + ")" + SUPPLIER_TYPE.getDescriptor();

                bsmArgs = new Object[]{
                        Type.getType("()Ljava/lang/Object;"),
                        new Handle(Opcodes.H_INVOKESPECIAL, type.getInternalName(), newName, m.desc),
                        Type.getType("()Ljava/lang/Object;")
                };

                mv.invokeDynamic("get", desc, bsm, bsmArgs);
            }else{
                final String desc = "(" + type.getDescriptor() + ")" + RUNNABLE_TYPE.getDescriptor();

                bsmArgs = new Object[]{
                        Type.getType("()V"),
                        new Handle(Opcodes.H_INVOKESPECIAL, type.getInternalName(), newName, m.desc),
                        Type.getType("()V")
                };

                mv.invokeDynamic("run", desc, bsm, bsmArgs);
            }

            //call constructor
            if(!hasReturnValue) {
                if(argumentTypes.length == 0) {
                    mv.invokeConstructor(SIMPLE_INTERCEPTION_TYPE, INTERCEPTION_CONSTRUCTOR1);
                }else{
                    mv.invokeConstructor(SIMPLE_INTERCEPTION_TYPE, INTERCEPTION_CONSTRUCTOR2);
                }
            }else{
                if(argumentTypes.length == 0) {
                    mv.invokeConstructor(SIMPLE_INTERCEPTION_TYPE, INTERCEPTION_CONSTRUCTOR3);
                }else{
                    mv.invokeConstructor(SIMPLE_INTERCEPTION_TYPE, INTERCEPTION_CONSTRUCTOR4);
                }
            }

            //store the interception
            int local = mv.newLocal(INTERCEPTOR_TYPE);
            mv.storeLocal(local);

            //call provider.run or runWithResult if has return value.
            mv.loadThis();
            mv.getField(type, PROVIDER_FIELD, PROVIDER_TYPE);
            mv.loadLocal(local);
            mv.invokeInterface(PROVIDER_TYPE, !hasReturnValue ? METHOD_PROVIDER_RUN1 : METHOD_PROVIDER_RUN2);

            if(hasReturnValue) {
                mv.checkCast(ASM.getReturnType(m));
            }

            mv.returnValue();

            mv.visitMaxs(0,0);
            mv.visitEnd();
        }

        protected void visitLambdaMethod(AopMethod am, String newName) {
            MethodNode m = am.getMethod();

            int    access = Opcodes.ACC_PRIVATE + Opcodes.ACC_SYNTHETIC;
            String name   = "lambda$" + newName;

            MethodVisitor real = cw.visitMethod(access, name, m.desc, null,
                    m.exceptions == null ? null : m.exceptions.toArray(Arrays2.EMPTY_STRING_ARRAY));

            GeneratorAdapter mv = new GeneratorAdapter(real, access, name, m.desc);

            mv.visitCode();

            mv.loadThis();
            mv.invokeVirtual(type, new Method(newName, m.desc));
            mv.returnValue();

            mv.visitMaxs(0,0);
            mv.visitEnd();
        }
    }

    protected static final class AopMethod extends AsmMethodInfo {

        private List<MethodInterceptorConfig> interceptors;

        public AopMethod(ClassNode c, MethodNode m) {
            super(c, m);
        }

    }

}