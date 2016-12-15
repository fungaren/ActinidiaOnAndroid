#include <jni.h>
#include "ActinidiaApi.h"

lua_State* L;

// lua: string OnCreate(), "" for success, msg for error
extern "C"
JNIEXPORT jstring JNICALL
Java_cc_moooc_actinidia_GameActivity_OnCreate(JNIEnv *env, jobject thiz){

    api_env = env;      // !!!
    api_thiz = thiz;
    jcls = env->GetObjectClass(thiz);

	L = luaL_newstate();
	luaL_openlibs(L);

	lua_register(L, "CreateImage", CreateImage);
	lua_register(L, "CreateImageEx", CreateImageEx);
	lua_register(L, "CreateTransImage", CreateTransImage);
	lua_register(L, "DeleteImage", DeleteImage);
	lua_register(L, "PrintText", PrintText);
	lua_register(L, "GetWidth", GetWidth);
	lua_register(L, "GetHeight", GetHeight);
	lua_register(L, "GetText", GetText);
	lua_register(L, "GetImage", GetImage);
	lua_register(L, "GetSound", GetSound);
	lua_register(L, "EmptyStack", EmptyStack);
	lua_register(L, "PasteToImage", PasteToImage);
	lua_register(L, "PasteToImageEx", PasteToImageEx);
	lua_register(L, "AlphaBlend", AlphaBlend);
	lua_register(L, "AlphaBlendEx", AlphaBlendEx);
	lua_register(L, "PasteToWnd", PasteToWnd);
	lua_register(L, "PasteToWndEx", PasteToWndEx);
	lua_register(L, "StopSound", StopSound);
	lua_register(L, "SetVolume", SetVolume);
	lua_register(L, "PlaySound", PlaySound);
    lua_register(L, "GetSetting", GetSetting);
    lua_register(L, "SaveSetting", SaveSetting);

    const char* main = "res/lua/main.lua";
    jstring jmain = env->NewStringUTF(main);
    jstring jstr = (jstring)env->CallObjectMethod(
            thiz, env->GetMethodID(jcls,"getText","(Ljava/lang/String;)Ljava/lang/String;"),
            jmain);
    // env->ReleaseStringUTFChars(jmain,main);
    env->DeleteLocalRef(jmain);
    int size = env->GetStringUTFLength(jstr);
    const char* buff = env->GetStringUTFChars(jstr, NULL);
	if (size > 0 && luaL_loadbuffer(L, buff, size, "line") == 0 && lua_pcall(L, 0, LUA_MULTRET, 0) == 0)
	{
        api_env->ReleaseStringUTFChars(jstr,buff);
        api_env->DeleteLocalRef(jstr);

		lua_getglobal(L, "core");
		lua_pushinteger(L, api_env->CallIntMethod(
                api_thiz, api_env->GetMethodID(jcls,"getScreenWidth","()I")));
		lua_setfield(L, -2, "screenwidth");
		lua_getglobal(L, "core");
		lua_pushinteger(L, api_env->CallIntMethod(
                api_thiz, api_env->GetMethodID(jcls,"getScreenHeight","()I")));
		lua_setfield(L, -2, "screenheight");

        lua_getglobal(L, "OnCreate");
        if (lua_pcall(L, 0, 1, 0)) {
            api_env->DeleteLocalRef(jcls);
            return env->NewStringUTF("");
        } else if (lua_isstring(L,-1)) {
            api_env->DeleteLocalRef(jcls);
            const char* luaErr = lua_tostring(L, -1);
            if (*luaErr == 0)
                return env->NewStringUTF("");
            else
                return env->NewStringUTF(luaErr);
        } else {
            api_env->DeleteLocalRef(jcls);
            return env->NewStringUTF("Wrong return type!");
        }
	} else {
        api_env->ReleaseStringUTFChars(jstr, buff);
        api_env->DeleteLocalRef(jstr);
        api_env->DeleteLocalRef(jcls);
        return env->NewStringUTF("Fail to load main script!");
    }
}

// lua: void OnPaint(WndGraphic)
extern "C"
JNIEXPORT jint JNICALL
Java_cc_moooc_actinidia_GameActivity_OnPaint(JNIEnv *env, jobject thiz, jobject WndGraphic)
{
    api_env = env;
    api_thiz = thiz;
    jcls = api_env->GetObjectClass(api_thiz);         // !!! do this every time

    lua_getglobal(L, "OnPaint");
    lua_pushinteger(L, (LUA_INTEGER)WndGraphic); // typedef _jobject* jobject;
    lua_pcall(L, 1, 0, 0);

    api_env->DeleteLocalRef(jcls);  // !!!
    return 0;
}

// lua: void OnClose()
extern "C"
JNIEXPORT jint JNICALL
Java_cc_moooc_actinidia_GameActivity_OnClose(JNIEnv *env, jobject thiz){
    api_env = env;
    api_thiz = thiz;
    jcls = api_env->GetObjectClass(api_thiz);         // !!!

    lua_getglobal(L, "OnClose");
    lua_pcall(L, 0, 0, 0);
    lua_close(L);
    return 0;
}

// lua: void OnLButtonDown(int x, int y)
extern "C"
JNIEXPORT jint JNICALL
Java_cc_moooc_actinidia_GameActivity_OnLButtonDown(JNIEnv *env, jobject thiz, jfloat x, jfloat y){
    api_env = env;
    api_thiz = thiz;
    jcls = api_env->GetObjectClass(api_thiz);         // !!!

    lua_getglobal(L, "OnLButtonDown");
    lua_pushinteger(L, (int)x);
    lua_pushinteger(L, (int)y);
    lua_pcall(L, 2, 0, 0);
    return 0;
}

// lua: void OnLButtonUp(int x, int y)
extern "C"
JNIEXPORT jint JNICALL
Java_cc_moooc_actinidia_GameActivity_OnLButtonUp(JNIEnv *env, jobject thiz, jfloat x, jfloat y){
    api_env = env;
    api_thiz = thiz;
    jcls = api_env->GetObjectClass(api_thiz);         // !!!

    lua_getglobal(L, "OnLButtonUp");
    lua_pushinteger(L, (int)x);
    lua_pushinteger(L, (int)y);
    lua_pcall(L, 2, 0, 0);
    return 0;
}

// lua: void OnMouseMove(int x, int y)
extern "C"
JNIEXPORT jint JNICALL
Java_cc_moooc_actinidia_GameActivity_OnMouseMove(JNIEnv *env, jobject thiz, jfloat x, jfloat y){
    api_env = env;
    api_thiz = thiz;
    jcls = api_env->GetObjectClass(api_thiz);         // !!! do this every time

    lua_getglobal(L, "OnMouseMove");
    lua_pushinteger(L, (int)x);
    lua_pushinteger(L, (int)y);
    lua_pcall(L, 2, 0, 0);
    return 0;
}
