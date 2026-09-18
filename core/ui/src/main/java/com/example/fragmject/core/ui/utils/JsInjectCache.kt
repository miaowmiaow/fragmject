package com.example.fragmject.core.ui.utils

import android.content.Context

/**
 * JS 脚本懒加载缓存：避免每次 WebView 加载新页面时都从 assets 中打开/读取/创建 String。
 * 这些脚本在应用生命周期内不变，启动后只读取一次，后续调用零 IO 开销。
 */
object JsInjectCache {
    @Volatile
    private var vConsoleJs: String? = null

    @Volatile
    private var quickVideoJs: String? = null

    @Volatile
    private var videoSaveJs: String? = null

    @Volatile
    private var videoScanJs: String? = null

    fun vConsoleJs(context: Context): String {
        return vConsoleJs ?: synchronized(this) {
            vConsoleJs ?: readAsset(context, "js/vconsole.min.js")?.let { js ->
                """
                    $js
                    var VConsole = new VConsole();
                """.trimIndent()
            }.also { vConsoleJs = it } ?: ""
        }
    }

    fun quickVideoJs(context: Context): String {
        return quickVideoJs ?: synchronized(this) {
            quickVideoJs ?: readAsset(context, "js/quick-video.js")?.trimIndent()
                .also { quickVideoJs = it } ?: ""
        }
    }

    /**
     * 视频长按检测脚本：监听 `<video>` / `<source>` / 主流播放器元素的长按事件，
     * 提取视频 src 并通过 VideoSaveBridge 回传原生层。
     *
     * 支持：HTML5 `<video>` / `<source>` / DPlayer / Hls.js / Video.js / Plyr / Aliplayer 等。
     * 使用 MutationObserver 监控动态创建的播放器元素，确保注入后创建的播放器也能被检测。
     */
    fun videoSaveJs(): String {
        return videoSaveJs ?: synchronized(this) {
            videoSaveJs ?: """
            (function() {
                if (window.__videoSaveInjected) return;
                window.__videoSaveInjected = true;
            
                var LONG_PRESS_MS = 600;
                var longPressTimer = null;
                var startX = 0, startY = 0;

                // 播放器包装器选择器：覆盖主流前端视频组件
                var PLAYER_WRAPPERS = [
                    '.dplayer-video', '.dplayer-video-wrap',   // DPlayer
                    '.prism-player', '.prism-video',            // Aliplayer
                    '.video-js', '.vjs-tech',                   // Video.js
                    '.plyr', '.plyr__video-wrapper',            // Plyr
                    '.artplayer-app', '.artplayer-video',       // ArtPlayer
                    '.xgplayer', '.xgplayer-video',             // 西瓜播放器
                    '.txp_player', '.txp_video',                // Tcplayer
                    '.chimee-container', '.chimee-video',       // Chimee
                    '.h5player', '.h5-video-player',            // 通用 H5 播放器
                    '.player-container', '.video-container',    // 通用容器
                    '[data-player]', '[data-video]',            // 通用 data 属性
                ];
                var WRAPPER_SEL = PLAYER_WRAPPERS.join(',');
            
                function getVideoSrc(video) {
                    var src = video.currentSrc || video.src || video.getAttribute('src') || '';
                    if (src && src !== window.location.href && !/^(blob|data):/i.test(src)) return src;
                    var sources = video.querySelectorAll('source');
                    for (var i = 0; i < sources.length; i++) {
                        var s = sources[i].src || sources[i].getAttribute('src') || '';
                        if (s && !/^(blob|data):/i.test(s)) return s;
                    }
                    // 播放器 data 属性兜底
                    var dataSrc = video.getAttribute('data-url') || video.getAttribute('data-video')
                        || video.getAttribute('data-src') || video.getAttribute('data-mp4');
                    if (dataSrc) return dataSrc;
                    return '';
                }
            
                function getWrapperSrc(el) {
                    // 先看包含的 video 元素
                    var videos = el.querySelectorAll('video');
                    for (var i = 0; i < videos.length; i++) {
                        var src = getVideoSrc(videos[i]);
                        if (src) return src;
                    }
                    // 再看容器自身的 data 属性
                    var ds = el.getAttribute('data-url') || el.getAttribute('data-video')
                        || el.getAttribute('data-src') || el.getAttribute('data-mp4');
                    return ds || '';
                }
            
                function findVideoAtPoint(x, y) {
                    // 先精确匹配 video 元素
                    var videos = document.querySelectorAll('video');
                    for (var i = 0; i < videos.length; i++) {
                        var rect = videos[i].getBoundingClientRect();
                        if (rect.width === 0 || rect.height === 0) continue;
                        if (x >= rect.left && x <= rect.right &&
                            y >= rect.top && y <= rect.bottom) {
                            return videos[i];
                        }
                    }
                    // 再匹配播放器包装器
                    var wrappers = document.querySelectorAll(WRAPPER_SEL);
                    for (var j = 0; j < wrappers.length; j++) {
                        try {
                            var r = wrappers[j].getBoundingClientRect();
                            if (r.width === 0 || r.height === 0) continue;
                            if (x >= r.left && x <= r.right && y >= r.top && y <= r.bottom) {
                                return wrappers[j];
                            }
                        } catch(e) {}
                    }
                    return null;
                }
            
                function onLongPress() {
                    var target = findVideoAtPoint(startX, startY);
                    if (!target) return;
                    var src = target.tagName === 'VIDEO' ? getVideoSrc(target) : getWrapperSrc(target);
                    if (src && window.VideoSaveBridge) {
                        window.VideoSaveBridge.onVideoLongPress(src);
                    }
                }
            
                function onPointerDown(e) {
                    startX = e.clientX;
                    startY = e.clientY;
                    longPressTimer = setTimeout(onLongPress, LONG_PRESS_MS);
                }
            
                function onPointerUp(e) { clear(); }
                function onPointerCancel(e) { clear(); }
                function onPointerMove(e) {
                    if (Math.abs(e.clientX - startX) > 10 || Math.abs(e.clientY - startY) > 10) clear();
                }
                function clear() { longPressTimer && clearTimeout(longPressTimer); longPressTimer = null; }
            
                // capture:true 在捕获阶段拦截，早于站点自己的 handlers
                ['pointerdown','pointerup','pointermove','pointercancel',
                 'touchstart','touchend','touchmove','touchcancel'].forEach(function(evt){
                    var handler = evt.indexOf('down')>-1||evt.indexOf('start')>-1 ? onPointerDown
                        : evt.indexOf('up')>-1||evt.indexOf('end')>-1 ? onPointerUp
                        : evt.indexOf('move')>-1 ? onPointerMove : onPointerCancel;
                    document.addEventListener(evt, handler, { capture: true, passive: true });
                });

                // MutationObserver：监控动态创建的播放器，无需额外操作
                try {
                    var observer = new MutationObserver(function(mutations) {
                        mutations.forEach(function(m) {
                            m.addedNodes.forEach(function(node) {
                                if (node.nodeType !== 1) return;
                                if (node.tagName === 'VIDEO' || node.querySelectorAll) {
                                    // 新元素已通过 WRAPPER_SEL 覆盖，只需确保 findVideoAtPoint 能匹配到
                                }
                            });
                        });
                    });
                    observer.observe(document.body || document.documentElement,
                        { childList: true, subtree: true });
                } catch(e) {}
            })();
            """.trimIndent().also { videoSaveJs = it }
        }
    }

    /**
     * 视频扫描脚本：遍历 document、iframe、inline scripts、主流播放器 JS 配置，
     * 提取所有可识别的视频 URL 并通过 VideoSaveBridge 回传原生层。
     *
     * 支持：
     * - HTML5 `<video>` / `<source>`
     * - DPlayer / Hls.js / Video.js / Plyr / ArtPlayer / Aliplayer / Tcplayer / Xgplayer
     * - Bilibili iframe 嵌入
     * - inline script / JSON 中的视频 URL
     * - 常见 data 属性（data-url / data-video / data-src / data-mp4）
     * - 同源 iframe 递归扫描（深度 3）
     */
    fun videoScanJs(): String {
        return videoScanJs ?: synchronized(this) {
            videoScanJs ?: """
            (function(){
                var urls=[];
                var VIDEO_EXT = /\.(mp4|m3u8|ts|flv|mkv|mov|avi|webm|mpd|m4s)(\?|$)/i;
                var BILIBILI_API = /bilibili\.com\/x\/player\/playurl/i;
                function isBlobOrData(s){return /^(blob|data):/i.test(s||'');}
                function isVideoUrl(s){
                    if(!s||isBlobOrData(s))return false;
                    if(BILIBILI_API.test(s))return true;
                    return VIDEO_EXT.test(s);
                }
                function addUrl(s){
                    if(s&&isVideoUrl(s)&&s!==window.location.href&&urls.indexOf(s)===-1)urls.push(s);
                }

                // ── 1) HTML5 video / source ──
                function scanDOM(doc){
                    var videos=doc.getElementsByTagName('video');
                    for(var i=0;i<videos.length;i++){
                        var v=videos[i];
                        var src=v.currentSrc||v.src||v.getAttribute('src')||'';
                        addUrl(src);
                        var dataSrc=v.getAttribute('data-url')||v.getAttribute('data-video')
                            ||v.getAttribute('data-src')||v.getAttribute('data-mp4');
                        addUrl(dataSrc);
                        var sources=v.getElementsByTagName('source');
                        for(var j=0;j<sources.length;j++){
                            var ss=sources[j].src||sources[j].getAttribute('src')||'';
                            addUrl(ss);
                        }
                    }
                    // 仅扫描可能包含视频 URL 的 data 属性（限定视频相关标签，避免 SPA 中泛 data 属性误匹配）
                    var dataEls=doc.querySelectorAll('video[data-url],video[data-video],video[data-src],video[data-mp4],'+
                        '[data-player][data-url],[data-player][data-video],[data-player][data-src],[data-player][data-mp4]');
                    for(var k=0;k<dataEls.length;k++){
                        var el=dataEls[k];
                        addUrl(el.getAttribute('data-url'));
                        addUrl(el.getAttribute('data-video'));
                        addUrl(el.getAttribute('data-src'));
                        addUrl(el.getAttribute('data-mp4'));
                    }
                }

                // ── 2) 扫描 HTML 原始文本中的视频 URL ──
                function scanHTML(doc){
                    try{
                        var html=doc.documentElement?doc.documentElement.outerHTML:'';
                        if(!html)return;
                        var re=/(https?:\/\/[^\s"'<>]+\.(?:mp4|m3u8|ts|flv|mkv|mov|avi|webm|mpd|m4s)(?:\?[^\s"'<>]*)?)/gi;
                        var m;while((m=re.exec(html))!==null){addUrl(m[1]);}
                    }catch(e){}
                }

                // ── 3) 扫描 inline script 中的主流播放器配置 ──
                function scanScripts(doc){
                    try{
                        var scripts=doc.getElementsByTagName('script');
                        var patterns=[
                            // DPlayer: new DPlayer({url:'...'}) 或 dp.url = '...'
                            /(?:url|video\.url|src)\s*[:=]\s*['"](https?:\/\/[^'"]+\.(?:mp4|m3u8|flv|mkv|webm|mov)[^'"]*)['"]/gi,
                            // Hls.js: new Hls({...}).loadSource('...') 或 source: '...'
                            /(?:loadSource|attachMedia)\s*\(\s*['"](https?:\/\/[^'"]*\.m3u8[^'"]*)['"]/gi,
                            // Aliplayer: source:'...'
                            /source\s*[:=]\s*['"](https?:\/\/[^'"]+\.(?:mp4|m3u8|flv)[^'"]*)['"]/gi,
                            // Tcplayer: fileID / url
                            /(?:fileID|url)\s*[:=]\s*['"](https?:\/\/[^'"]+\.(?:mp4|m3u8|flv)[^'"]*)['"]/gi,
                            // Xgplayer / ArtPlayer / Plyr: url: '...'
                            /['"]url['"]\s*:\s*['"](https?:\/\/[^'"]+\.(?:mp4|m3u8|flv|mkv|webm)[^'"]*)['"]/gi,
                        ];
                        for(var i=0;i<scripts.length;i++){
                            var text=scripts[i].textContent||scripts[i].innerHTML||'';
                            if(!text)continue;
                            for(var j=0;j<patterns.length;j++){
                                var m;while((m=patterns[j].exec(text))!==null){
                                    addUrl(m[1]||m[0]);
                                }
                            }
                        }
                    }catch(e){}
                }

                // ── 4) 扫描全局播放器实例 ──
                function scanGlobalPlayers(doc){
                    try{
                        var win=doc.defaultView||doc.parentWindow||window;
                        // DPlayer instances
                        if(win.dplayer||win.dp){
                            var dp=win.dplayer||win.dp;
                            addUrl(typeof dp==='object'?dp.url||dp.video||dp.videoUrl:'');
                        }
                        // Aliplayer
                        if(win.player||win.aliplayer){
                            var ap=win.player||win.aliplayer;
                            if(ap&&ap._config)addUrl(ap._config.source||ap._config.url);
                        }
                        // Video.js players
                        if(win.videojs){
                            try{
                                var vjsPlayers=win.videojs.getAllPlayers?win.videojs.getAllPlayers():[];
                                for(var i=0;i<vjsPlayers.length;i++){
                                    var p=vjsPlayers[i];
                                    addUrl(p.currentSource?p.currentSource().src:'');
                                    addUrl(p.src?p.src():'');
                                }
                            }catch(e){}
                        }
                        // ArtPlayer
                        if(win.art||win.Artplayer){
                            var art=win.art||win.Artplayer;
                            if(art&&art.instances){
                                for(var j=0;j<art.instances.length;j++){
                                    addUrl(art.instances[j].option?art.instances[j].option.url:'');
                                }
                            }
                        }
                    }catch(e){}
                }

                // ── 5) Bilibili / iframe 嵌入提取 ──
                function extractBilibiliUrl(doc){
                    try{
                        var iframes=doc.getElementsByTagName('iframe');
                        for(var i=0;i<iframes.length;i++){
                            var src=iframes[i].src||iframes[i].getAttribute('src')||'';
                            if(!src)continue;
                            // Bilibili: //player.bilibili.com/player.html?aid=...&cid=...&bvid=...
                            if(/bilibili\.com\/player/i.test(src)){
                                // 构造直接可下载的 B 站视频 API URL
                                var m=src.match(/(?:aid|avid)=(\d+)/);
                                var cidMatch=src.match(/cid=(\d+)/);
                                var bvidMatch=src.match(/bvid=(\w+)/);
                                if(m&&cidMatch){
                                    addUrl('https://api.bilibili.com/x/player/playurl?avid='+m[1]+'&cid='+cidMatch[1]+'&qn=80&type=&otype=json');
                                }
                                if(bvidMatch&&cidMatch){
                                    addUrl('https://api.bilibili.com/x/player/playurl?bvid='+bvidMatch[1]+'&cid='+cidMatch[1]+'&qn=80&type=&otype=json');
                                }
                            }
                        }
                    }catch(e){}
                }

                // ── 6) 递归扫描同源 iframe ──
                function scanAll(doc,depth){
                    if(!doc||depth>3)return;
                    try{
                        scanDOM(doc);
                        scanHTML(doc);
                        scanScripts(doc);
                        scanGlobalPlayers(doc);
                        extractBilibiliUrl(doc);
                    }catch(e){}
                    try{
                        var iframes=doc.getElementsByTagName('iframe');
                        for(var k=0;k<iframes.length;k++){
                            try{
                                var d=iframes[k].contentDocument||iframes[k].contentWindow.document;
                                if(d)scanAll(d,depth+1);
                            }catch(e){}
                        }
                    }catch(e){}
                }

                scanAll(document,0);
                if(urls.length>0)window.VideoSaveBridge&&window.VideoSaveBridge.onVideoLongPress(urls.join('|'));
                else window.VideoSaveBridge&&window.VideoSaveBridge.onVideoLongPress('');
            })();
            """.trimIndent().also { videoScanJs = it }
        }
    }

    private fun readAsset(context: Context, path: String): String? {
        return try {
            val bytes = context.resources.assets.open(path).use { input ->
                ByteArray(input.available()).also { input.read(it) }
            }
            String(bytes)
        } catch (_: Exception) {
            null
        }
    }
}

/** @deprecated 改用 [JsInjectCache.vConsoleJs]，避免每次打开 assets 文件。 */
@Deprecated("Use JsInjectCache.vConsoleJs()", ReplaceWith("JsInjectCache.vConsoleJs(this)"))
fun Context.injectVConsoleJs(): String = JsInjectCache.vConsoleJs(this)

/** @deprecated 改用 [JsInjectCache.quickVideoJs]，避免每次打开 assets 文件。 */
@Deprecated("Use JsInjectCache.quickVideoJs()", ReplaceWith("JsInjectCache.quickVideoJs(this)"))
fun Context.injectQuickVideoJs(): String = JsInjectCache.quickVideoJs(this)

/** @deprecated 改用 [JsInjectCache.videoSaveJs]，避免每次打开 assets 文件。 */
@Deprecated("Use JsInjectCache.videoSaveJs()", ReplaceWith("JsInjectCache.videoSaveJs(this)"))
fun Context.injectVideoSaveJs(): String = JsInjectCache.videoSaveJs()

/** 获取视频扫描脚本（一次性主动扫描所有 `<video>` 元素及其 `src`）。 */
fun videoScanJs(): String = JsInjectCache.videoScanJs()