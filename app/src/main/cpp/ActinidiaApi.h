#ifndef ACTINIDIA_ACTINIDIAAPI_H
#define ACTINIDIA_ACTINIDIAAPI_H

#include <lua.hpp>

JNIEnv *api_env;
jobject api_thiz;
jclass jcls;

// Global functions for lua

// lua: Image CreateImage(width, height)
int CreateImage(lua_State *L)
{
    int n = lua_gettop(L);
    if (n != 2) return 0;
    int width = (int)lua_tointeger(L, 1);
    int height = (int)lua_tointeger(L, 2);
    lua_pop(L, 2);
    jobject g = api_env->CallObjectMethod(
            api_thiz, api_env->GetMethodID(jcls,"createImage","(II)Landroid/graphics/Bitmap;"),
            width, height);
    g = api_env->NewGlobalRef(g);   // !!!
    lua_pushinteger(L, (LUA_INTEGER)g);
    return 1;
}

// lua: Image CreateImageEx(width, height, color)
int CreateImageEx(lua_State *L)
{
    int n = lua_gettop(L);
    if (n != 3) return 0;
    int width = (int)lua_tointeger(L, 1);
    int height = (int)lua_tointeger(L, 2);
    LUA_INTEGER color = lua_tointeger(L, 3);
    lua_pop(L, 3);
    jobject g = api_env->CallObjectMethod(
            api_thiz, api_env->GetMethodID(jcls,"createImageEx","(IIIIII)Landroid/graphics/Bitmap;"),
            width, height, (char)color, (char)(color>>8), (char)(color>>16), (char)(color>>24));
    g = api_env->NewGlobalRef(g);   // !!!
    lua_pushinteger(L, (LUA_INTEGER)g);
    return 1;
}

// lua: Image CreateTransImage(width, height)
int CreateTransImage(lua_State *L)
{
    int n = lua_gettop(L);
    if (n != 2) return 0;
    int width = (int)lua_tointeger(L, 1);
    int height = (int)lua_tointeger(L, 2);
    lua_pop(L, 2);
    jobject g = api_env->CallObjectMethod(
            api_thiz, api_env->GetMethodID(jcls,"createTransImage","(II)Landroid/graphics/Bitmap;"),
            width, height);
    g = api_env->NewGlobalRef(g);   // !!!
    lua_pushinteger(L, (LUA_INTEGER)g);
    return 1;
}

// lua: void DeleteImage(g)
int DeleteImage(lua_State *L)
{
    int n = lua_gettop(L);
    if (n != 1) return 0;
    jobject g = (jobject)lua_tointeger(L, 1);
    lua_pop(L, 1);
    if (g!=NULL)
        api_env->DeleteGlobalRef(g);   // !!!
    return 0;
}

// lua: int GetWidth(g)
int GetWidth(lua_State *L)
{
    int n = lua_gettop(L);
    if (n != 1) return 0;
    jobject g = (jobject)lua_tointeger(L, 1);
    jint w = api_env->CallIntMethod(
            api_thiz, api_env->GetMethodID(jcls,"getWidth","(Landroid/graphics/Bitmap;)I"),
            g);
    lua_pop(L, 1);
    lua_pushinteger(L, w);
    return 1;
}

// lua: int GetHeight(g)
int GetHeight(lua_State *L)
{
    int n = lua_gettop(L);
    if (n != 1) return 0;
    jobject g = (jobject)lua_tointeger(L, 1);
    jint h = api_env->CallIntMethod(
            api_thiz, api_env->GetMethodID(jcls,"getHeight","(Landroid/graphics/Bitmap;)I"),
            g);
    lua_pop(L, 1);
    lua_pushinteger(L, h);
    return 1;
}

// lua: string GetText(pathname)
int GetText(lua_State *L)
{
    int n = lua_gettop(L);
    if (n != 1) return 0;
    const char* f = lua_tostring(L, 1);
    lua_pop(L, 1);
    jstring jf = api_env->NewStringUTF(f);
    jstring jstr = (jstring)api_env->CallObjectMethod(
            api_thiz, api_env->GetMethodID(jcls,"getText","(Ljava/lang/String;)Ljava/lang/String;"),
            jf);
    int size = api_env->GetStringUTFLength(jstr);
    if (size > 0)
    {
        const char* str = api_env->GetStringUTFChars(jstr, NULL);
        lua_pushlstring(L, str, size);
        api_env->ReleaseStringUTFChars(jstr,str);
        api_env->DeleteLocalRef(jstr);
        return 1;
    }
    lua_pushnil(L);
    return 1;
}

// lua: image GetImage(pathname)
int GetImage(lua_State *L)
{
    int n = lua_gettop(L);
    if (n != 1) return 0;
    const char* f = lua_tostring(L, 1);
    lua_pop(L, 1);
    jstring jf = api_env->NewStringUTF(f);
    jobject img = (jobject)api_env->CallObjectMethod(
            api_thiz, api_env->GetMethodID(jcls,"getImage",
                                           "(Ljava/lang/String;)Landroid/graphics/Bitmap;"),
            jf);
    if (NULL!=img)
    {
        img = api_env->NewGlobalRef(img);
        lua_pushinteger(L, (LUA_INTEGER)img);
        return 1;
    }
    lua_pushnil(L);
    return 1;
}

// lua: sound GetSound(pathname, b_loop)
int GetSound(lua_State *L)
{
    int n = lua_gettop(L);
    if (n != 2) return 0;
    const char* f = lua_tostring(L, 1);
    jstring jf = api_env->NewStringUTF(f);
    int b_loop = lua_toboolean(L, 2);
    lua_pop(L, 2);
    jint sound = api_env->CallIntMethod(
            api_thiz, api_env->GetMethodID(jcls,"getSound",
                                           "(Ljava/lang/String;Z)I"),
            jf, b_loop);
    if (sound!=NULL)
    {
        lua_pushinteger(L, (LUA_INTEGER)sound);
        return 1;
    }
    lua_pushnil(L);
    return 1;
}

// lua: void EmptyStack()
int EmptyStack(lua_State *L)
{
    return 0;
}

// lua: void PasteToImage(gDest, gSrc, xDest, yDest)
int PasteToImage(lua_State *L)
{
    int n = lua_gettop(L);
    if (n != 4) return 0;
    jobject gDest = (jobject)lua_tointeger(L, 1);
    jobject gSrc = (jobject)lua_tointeger(L, 2);
    float x = (float)lua_tonumber(L, 3);
    float y = (float)lua_tonumber(L, 4);
    lua_pop(L, 4);
    api_env->CallVoidMethod(
            api_thiz,api_env->GetMethodID(jcls,"pasteToImage",
                                          "(Landroid/graphics/Bitmap;Landroid/graphics/Bitmap;FF)V"),
            gDest, gSrc, x, y
    );
    return 0;
}

// lua: void PasteToImageEx(gDest, gSrc, xDest, yDest, DestWidth, DestHeight, xSrc, ySrc, SrcWidth, SrcHeight)
int PasteToImageEx(lua_State *L)
{
    int n = lua_gettop(L);
    if (n != 10) return 0;
    jobject gDest = (jobject)lua_tointeger(L, 1);
    jobject gSrc = (jobject)lua_tointeger(L, 2);
    int xDest = (int)lua_tointeger(L, 3);
    int yDest = (int)lua_tointeger(L, 4);
    int DestWidth = (int)lua_tointeger(L, 5);
    int DestHeight = (int)lua_tointeger(L, 6);
    int xSrc = (int)lua_tointeger(L, 7);
    int ySrc = (int)lua_tointeger(L, 8);
    int SrcWidth = (int)lua_tointeger(L, 9);
    int SrcHeight = (int)lua_tointeger(L, 10);
    lua_pop(L, 10);
    api_env->CallVoidMethod(
            api_thiz,api_env->GetMethodID(jcls,"pasteToImageEx",
                                          "(Landroid/graphics/Bitmap;Landroid/graphics/Bitmap;IIIIIIII)V"),
            gDest, gSrc, xDest, yDest, DestWidth, DestHeight, xSrc, ySrc, SrcWidth, SrcHeight
    );
    return 0;
}

// lua: void AlphaBlend(gDest, gSrc, xDest, yDest, SrcAlpha)
int AlphaBlend(lua_State *L)
{
    int n = lua_gettop(L);
    if (n != 5) return 0;
    jobject gDest = (jobject)lua_tointeger(L, 1);
    jobject gSrc = (jobject)lua_tointeger(L, 2);
    int xDest = (int)lua_tointeger(L, 3);
    int yDest = (int)lua_tointeger(L, 4);
    char SrcAlpha = (char)lua_tointeger(L, 5);
    lua_pop(L, 5);
    api_env->CallVoidMethod(
            api_thiz,api_env->GetMethodID(jcls,"alphaBlend",
                                          "(Landroid/graphics/Bitmap;Landroid/graphics/Bitmap;III)V"),
            gDest, gSrc, xDest, yDest, (jint)SrcAlpha
    );
    return 0;
}

// lua: void AlphaBlendEx(gDest, gSrc, xDest, yDest, DestWidth, DestHeight, xSrc, ySrc, SrcWidth, SrcHeight, SrcAlpha)
int AlphaBlendEx(lua_State *L)
{
    int n = lua_gettop(L);
    if (n != 11) return 0;
    jobject gDest = (jobject)lua_tointeger(L, 1);
    jobject gSrc = (jobject)lua_tointeger(L, 2);
    int xDest = (int)lua_tointeger(L, 3);
    int yDest = (int)lua_tointeger(L, 4);
    int DestWidth = (int)lua_tointeger(L, 5);
    int DestHeight = (int)lua_tointeger(L, 6);
    int xSrc = (int)lua_tointeger(L, 7);
    int ySrc = (int)lua_tointeger(L, 8);
    int SrcWidth = (int)lua_tointeger(L, 9);
    int SrcHeight = (int)lua_tointeger(L, 10);
    char SrcAlpha = (char)lua_tointeger(L, 11);
    lua_pop(L, 11);
    api_env->CallVoidMethod(
            api_thiz,api_env->GetMethodID(jcls,"alphaBlendEx",
                                          "(Landroid/graphics/Bitmap;Landroid/graphics/Bitmap;IIIIIIIII)V"),
            gDest, gSrc, xDest, yDest, DestWidth, DestHeight, xSrc, ySrc, SrcWidth, SrcHeight, (jint)SrcAlpha
    );
    return 0;
}

// lua: void PasteToWnd(WndGraphic, g)
int PasteToWnd(lua_State *L)
{
    int n = lua_gettop(L);
    if (n != 2) return 0;
    jobject WndGraphic = (jobject)lua_tointeger(L, 1);
    jobject g = (jobject)lua_tointeger(L, 2);
    lua_pop(L, 2);
    api_env->CallVoidMethod(
            api_thiz,api_env->GetMethodID(jcls,"pasteToWnd",
                                          "(Landroid/graphics/Canvas;Landroid/graphics/Bitmap;)V"),
            WndGraphic, g
    );
    return 0;
}

// lua: void PasteToWndEx(WndGraphic, g, xDest, yDest, DestWidth, DestHeight, xSrc, ySrc, SrcWidth, SrcHeight)
int PasteToWndEx(lua_State *L)
{
    int n = lua_gettop(L);
    if (n != 10) return 0;
    jobject WndGraphic = (jobject)lua_tointeger(L, 1);
    jobject g = (jobject)lua_tointeger(L, 2);
    int xDest = (int)lua_tointeger(L, 3);
    int yDest = (int)lua_tointeger(L, 4);
    int DestWidth = (int)lua_tointeger(L, 5);
    int DestHeight = (int)lua_tointeger(L, 6);
    int xSrc = (int)lua_tointeger(L, 7);
    int ySrc = (int)lua_tointeger(L, 8);
    int SrcWidth = (int)lua_tointeger(L, 9);
    int SrcHeight = (int)lua_tointeger(L, 10);
    lua_pop(L, 10);
    api_env->CallVoidMethod(
            api_thiz,api_env->GetMethodID(jcls,"pasteToWndEx",
                                          "(Landroid/graphics/Canvas;Landroid/graphics/Bitmap;IIIIIIII)V"),
            WndGraphic, g, xDest, yDest, DestWidth, DestHeight, xSrc, ySrc, SrcWidth, SrcHeight
    );
    return 0;
}

// lua: void StopSound(sound)
int StopSound(lua_State *L)
{
    int n = lua_gettop(L);
    if (n != 1) return 0;
    jint sound = (jint)lua_tointeger(L, 1);
    lua_pop(L, 1);
    api_env->CallVoidMethod(
            api_thiz,api_env->GetMethodID(jcls,"stopSound", "(I)V"), sound);
    return 0;
}

// lua: void SetVolume(sound,volume), volume: 0-1
int SetVolume(lua_State *L)
{
    int n = lua_gettop(L);
    if (n != 2) return 0;
    jint sound = (jint)lua_tointeger(L, 1);
    float volume = (float)lua_tonumber(L, 2);
    lua_pop(L, 2);
    api_env->CallVoidMethod(
            api_thiz,api_env->GetMethodID(jcls,"setVolume", "(IF)V"), sound, volume);
    return 0;
}

// lua: void PlaySound(sound)
int PlaySound(lua_State *L)
{
    int n = lua_gettop(L);
    if (n != 1) return 0;
    jint sound = (jint)lua_tointeger(L, 1);
    lua_pop(L, 1);
    api_env->CallVoidMethod(
            api_thiz,api_env->GetMethodID(jcls,"playSound", "(I)V"), sound);
    return 0;
}

// lua: string GetSetting(string key)
int GetSetting(lua_State *L)
{
    int n = lua_gettop(L);
    if (n != 1) return 0;
    const char* key = lua_tostring(L, 1);
    lua_pop(L, 1);
    jstring jkey = api_env->NewStringUTF(key);
    jstring jval = (jstring)api_env->CallObjectMethod(
            api_thiz, api_env->GetMethodID(jcls,"getSetting",
                                           "(Ljava/lang/String;)Ljava/lang/String;"),
            jkey);
    if (NULL!=jval)
    {
        const char* str = api_env->GetStringUTFChars(jval, NULL);
        lua_pushlstring(L, str, (size_t)api_env->GetStringUTFLength(jval));
        api_env->ReleaseStringUTFChars(jval,str);
        api_env->DeleteLocalRef(jval);
        return 1;
    }
    lua_pushnil(L);
    return 1;
}

// lua: void SaveSetting(string key, string value)
int SaveSetting(lua_State *L)
{
    int n = lua_gettop(L);
    if (n != 2) return 0;
    const char* key = lua_tostring(L, 1);
    const char* val = lua_tostring(L, 2);
    lua_pop(L, 2);
    jstring jkey = api_env->NewStringUTF(key);
    jstring jval = api_env->NewStringUTF(val);
    api_env->CallVoidMethod(
            api_thiz, api_env->GetMethodID(jcls,"saveSetting",
                                           "(Ljava/lang/String;Ljava/lang/String;)V"),
            jkey, jval);
    return 0;
}

#endif //ACTINIDIA_ACTINIDIAAPI_H