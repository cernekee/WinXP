/*
 * Win XPrivacy: Bypassing XPrivacy hooks using JNI
 *
 * Copyright (c) 2014, Kevin Cernekee <cernekee@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

#include <stdlib.h>
#include <dlfcn.h>
#include <android/log.h>
#include <jni.h>

#define PACKAGE		"com/example/winxp/Native"
#define MAGIC		"some magic string that XPrivacy will never guess"

#define ANDROID_SMP	1
#define INLINE		inline

#include <stdint.h>
#include <pthread.h>
#include "DvmDex.h"
#include "libdex/DexProto.h"
#include "Object.h"

struct XposedHookInfo {
	struct {
		Method originalMethod;
		// copy a few bytes more than defined for Method in AOSP
		// to accomodate for (rare) extensions by the target ROM
		int dummyForRomExtensions[4];
	} originalMethodStruct;

	Object* reflectedMethod;
	Object* additionalInfo;
};

static Thread *(*dvmThreadSelf)(void);
static Object *(*dvmDecodeIndirectRef)(Thread* self, jobject jobj);

static jboolean JNICALL init(JNIEnv *jenv, jclass jclazz)
{
	dvmThreadSelf = (Thread *(*)())
		dlsym(RTLD_DEFAULT, "_Z13dvmThreadSelfv");
	dvmDecodeIndirectRef = (Object *(*)(Thread *, jobject))
		dlsym(RTLD_DEFAULT, "_Z20dvmDecodeIndirectRefP6ThreadP8_jobject");
	if (!dvmThreadSelf || !dvmDecodeIndirectRef) {
		return 0;
	}
	return 1;
}

static jstring JNICALL getMagic(JNIEnv *jenv, jclass jclazz)
{
	return jenv->NewStringUTF(MAGIC);
}

static void fix_method(Method *m)
{
	XposedHookInfo *hookInfo = (XposedHookInfo *)m->insns;
	Method *orig = &hookInfo->originalMethodStruct.originalMethod;

	CLEAR_METHOD_FLAG(m, ACC_NATIVE);
	m->nativeFunc = orig->nativeFunc;
	m->insns = orig->insns;
	m->registersSize = orig->registersSize;
	m->outsSize = orig->outsSize;
}

static jboolean JNICALL nukeXposed(JNIEnv *jenv, jclass jclazz, jstring jstr)
{
	jclass jcls = NULL;
	const char *str;

	str = jenv->GetStringUTFChars(jstr, NULL);
	if (!str) {
		return 0;
	}

	jcls = jenv->FindClass(str);
	jenv->ReleaseStringUTFChars(jstr, str);

	if (jcls == NULL) {
		return 0;
	}

	ClassObject *cl = (ClassObject *)dvmDecodeIndirectRef(dvmThreadSelf(),
		jcls);
	if (cl == NULL) {
		return 0;
	}

	Method *m;
	for (int i = 0; i < cl->directMethodCount; i++) {
		m = &cl->directMethods[i];
		if (IS_METHOD_FLAG_SET(m, ACC_NATIVE)) {
			fix_method(m);
		}
	}
	for (int i = 0; i < cl->virtualMethodCount; i++) {
		m = &cl->virtualMethods[i];
		if (IS_METHOD_FLAG_SET(m, ACC_NATIVE)) {
			fix_method(m);
		}
	}

	return 1;
}

static const JNINativeMethod methods[] = {
	{ "init",       "()Z",                   (void *)init },
	{ "getMagic",   "()Ljava/lang/String;",  (void *)getMagic },
	{ "nukeXposed", "(Ljava/lang/String;)Z", (void *)nukeXposed },
};

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
	JNIEnv* env;
	if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) !=
	    JNI_OK) {
		return -1;
	}

	jclass jcls = env->FindClass(PACKAGE);
	if (!jcls) {
		return -1;
	}

	env->RegisterNatives(jcls, methods, sizeof(methods) / sizeof(methods[0]));

	return JNI_VERSION_1_6;
}
