# Spotify Remake

Spring Boot + Vue 3 前后端分离的音乐流媒体平台，包含用户客户端和 CMS 管理后台。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端框架 | Spring Boot 3.2.0, Java 21 |
| ORM | MyBatis 3.0.3 |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis (Lettuce) + Caffeine (L1 本地缓存) + Redisson (布隆过滤器/分布式锁) |
| 安全 | Spring Security + JWT + AJ-Captcha 滑块验证码 |
| 对象存储 | 阿里云 OSS |
| 外部音乐源 | GD Music API (网易云等) |
| 用户前端 | Vue 3 + Vite 8 |
| CMS 前端 | Vue 3 + Vite 6 + Tailwind CSS 4 |
| 构建工具 | Maven |

---

## 项目结构

```
Spotify_Remake/
├── pom.xml                                    # Maven 配置
├── database_spotify.sql                       # 完整数据库建表脚本
├── application.yaml                           # ⚠️ 已 gitignore，需手动创建
├── application-template.yaml                  # 配置模板（不含敏感信息）
│
├── src/main/java/com/ty1l/spotify_remake/
│   ├── SpotifyRemakeApplication.java          # 启动类
│   ├── config/                                # 配置类
│   │   ├── SecurityConfig.java                # Spring Security 路由放行
│   │   ├── WebConfig.java                     # CORS、拦截器、静态资源映射
│   │   ├── RedisConfig.java                   # Redis 序列化配置
│   │   ├── RedissonConfig.java                # Redisson 连接配置
│   │   ├── CaffeineConfig.java                # L1 本地缓存配置
│   │   ├── OssConfig.java                     # 阿里云 OSS 配置
│   │   ├── AsyncConfig.java                   # 异步线程池配置
│   │   └── CaptchaConfig.java                 # 验证码配置
│   ├── interceptor/                           # 请求拦截器
│   │   ├── TokenInterceptor.java              # 用户端 JWT 校验
│   │   └── AdminTokenInterceptor.java         # CMS 端 JWT 校验
│   ├── Controller/
│   │   ├── User/                              # 用户端接口 (登录/注册/歌单/主页/个人资料)
│   │   ├── Admin/                             # CMS 端接口 (管理登录/歌单/歌曲/艺术家)
│   │   └── Public/                            # 公开接口 (搜索/歌曲/歌单/艺术家/外部音乐/歌词/排行榜/歌曲流)
│   ├── Service/
│   │   ├── User/                              # 用户服务
│   │   ├── Admin/                             # CMS 服务
│   │   ├── Public/                            # 公共服务 (含 External/ — GD Music API 客户端)
│   │   ├── CacheService.java                  # L2 Redis 缓存服务
│   │   ├── L1CacheService.java                # L1 Caffeine 缓存服务
│   │   └── CacheLockHelper.java               # 缓存击穿防护（分布式锁）
│   ├── Entity/
│   │   ├── User/                              # 用户实体 & VO
│   │   ├── Admin/                             # 管理员实体
│   │   └── Public/                            # 公共实体 (Song, Artist, Album 等)
│   ├── Mapper/                                # MyBatis Mapper 接口
│   └── utility/                               # 工具类 (JWT, 文件上传, 统一响应体等)
│
├── src/main/resources/
│   ├── application.yaml                       # 主配置文件
│   ├── application-template.yaml              # 配置模板
│   └── com/ty1l/spotify_remake/Mapper/*.xml   # MyBatis XML Mapper
│
└── src/main/resources/static/
    ├── spotify-frontend/                      # 用户前端 (Vue 3)
    │   ├── src/
    │   │   ├── components/                    # Vue 组件 (AuthPage, MainApp, CaptchaModal 等)
    │   │   ├── api/auth.js                    # API 请求封装
    │   │   ├── stores/                        # 状态管理 (播放、最近播放、存储)
    │   │   ├── CSS/                           # 样式文件
    │   │   └── utils/                         # 工具函数
    │   └── vite.config.js                     # Vite 配置（端口 8000）
    │
    └── spotify-cms/                           # CMS 后台前端 (Vue 3 + Tailwind)
        ├── src/
        │   ├── views/                         # 页面视图 (Login, Dashboard, Songs, Playlists, Artists, Albums)
        │   ├── components/                    # 业务组件
        │   ├── composables/                   # 组合式函数
        │   └── useApi.js                      # API 请求封装
        └── vite.config.js                     # Vite 配置（端口 4000）
  
```

---

## 环境要求

| 依赖 | 最低版本 | 说明 |
|---|---|---|
| JDK | 21 | 需支持虚拟线程（`spring.threads.virtual.enabled=true`） |
| Maven | 3.8+ | 构建后端 |
| Node.js | 18+ | 构建前端（推荐 20 LTS） |
| MySQL | 8.0 | 数据库引擎需支持 `utf8mb4` |
| Redis | 6.0+ | 用于 JWT 版本校验、验证码缓存、布隆过滤器 |

---

## 快速部署

### 1. 克隆项目

```bash
git clone <仓库地址>
cd Spotify_Remake
```

### 2. 创建数据库

```bash
mysql -u root -p < database_spotify.sql
```

脚本会自动创建 `database_spotify` 数据库，包含以下表：

| 表名 | 说明 |
|---|---|
| `user` | 用户账号 |
| `sys_admin` | CMS 管理员 |
| `songs` | 歌曲 |
| `song_artist` | 歌曲-艺术家关联 |
| `albums` | 专辑 |
| `artists` | 艺术家 |
| `playlists` | 歌单 |
| `playlist_songs` | 歌单-歌曲关联 |
| `song_play_log` | 播放记录 |
| `user_playback_history` | 用户播放历史 |
| `user_collected_playlists` | 用户收藏歌单 |
| `user_artist_follows` | 用户关注艺术家 |

### 3. 安装并启动 Redis

确保 Redis 在本地运行，默认连接 `127.0.0.1:6379`：

```bash
redis-server
```

> 验证码缓存、JWT 版本校验、布隆过滤器均依赖 Redis，不启动 Redis 将导致服务启动失败。

### 4. 配置后端

从模板创建 `application.yaml`：

```bash
cp src/main/resources/application-template.yaml src/main/resources/application.yaml
```

需要修改的配置项：

```yaml
server:
  port: 5000                               # 用户端后端端口

spring:
  datasource:
    url: jdbc:mysql://<你的MySQL地址>:3306/database_spotify
    username: <数据库用户名>
    password: <数据库密码>
  data:
    redis:
      host: <你的Redis地址>
      port: 6379

# 阿里云 OSS（文件上传需要）
aliyun:
  oss:
    endpoint: <OSS Endpoint>
    access-key-id: <AccessKey ID>
    access-key-secret: <AccessKey Secret>
    bucket-name: <Bucket 名称>
    region: <地域>

# 外部音乐 API（可选，默认 GD Music Studio）
music:
  external:
    gdmusic:
      base-url: https://music-api.gdstudio.xyz
      source: netease
```

> **阿里云 OSS** 用于用户头像、歌曲封面、歌词文件等的上传存储。如果不配置 OSS，文件上传功能将不可用。

### 5. 构建后端

```bash
mvn clean package -DskipTests
```

### 6. 构建前端

**用户前端：**

```bash
cd src/main/resources/static/spotify-frontend
npm install
npm run build
```

构建产物输出到 `dist/` 目录，后端通过 `/spotify-frontend/**` 路径映射到此目录。

**CMS 前端：**

```bash
cd src/main/resources/static/spotify-cms
npm install
npm run build
```

构建产物输出到 `../spotify-cms-dist/`，后端通过 `/spotify-cms/**` 路径映射。

### 7. 启动后端

```bash
java -jar target/Spotify_Remake-0.0.1-SNAPSHOT.jar
```

### 8. 访问

| 端面 | 地址 |
|---|---|
| 用户客户端 | http://localhost:5000/ |
| CMS 管理后台 | http://localhost:5000/spotify-cms/ |

> CMS 后端接口路径为 `/admin/spotify/**`，与用户端共享同一服务实例。

---

## 开发模式

开发时可以单独启动前端 dev server 以支持热更新：

```bash
# 终端 1：启动后端
mvn spring-boot:run

# 终端 2：用户前端（端口 8000，自动代理 API 到 5000）
cd src/main/resources/static/spotify-frontend
npm run dev

# 终端 3：CMS 前端（端口 4000，注意 CMS 的 API 代理目标）
cd src/main/resources/static/spotify-cms
npm run dev
```

用户前端 vite.config.js 已配置代理，`/api/**` 请求自动转发到 `localhost:5000`。

---

## 配置详解

### application.yaml 完整说明

```yaml
server:
  port: 5000                    # 后端服务端口

spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://<host>:3306/database_spotify
    username: <user>
    password: <password>
  servlet:
    multipart:
      max-file-size: 50MB       # 单文件上传最大 50MB（音乐文件较大）
      max-request-size: 100MB   # 单次请求最大 100MB
  threads:
    virtual:
      enabled: true             # 启用 Java 21 虚拟线程
  data:
    redis:
      host: <redis-host>
      port: 6379
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: 100

mybatis:
  mapper-locations: classpath:com/ty1l/spotify_remake/Mapper/*.xml
  type-aliases-package: com.ty1l.spotify_remake.Entity
  configuration:
    map-underscore-to-camel-case: true   # 下划线自动转驼峰

aj:
  captcha:
    type: blockpuzzle            # 滑块拼图验证码
    cache-type: redis            # 验证码存 Redis
    watermark: "MyProject"       # 验证码水印文字
    aes-status: true             # 坐标加密

music:
  external:
    gdmusic:
      base-url: https://music-api.gdstudio.xyz   # GD Music API 地址
      source: netease                             # 音源（netease/qq/kugou 等）

aliyun:
  oss:
    endpoint: <OSS Endpoint>
    access-key-id: <AccessKey ID>
    access-key-secret: <AccessKey Secret>
    bucket-name: <Bucket>
    region: <Region>
```

### 缓存架构

系统采用 **L1 + L2 双级缓存**：

| 层级 | 存储 | TTL | 用途 |
|---|---|---|---|
| L1 | Caffeine (JVM 内存) | 5 分钟 | 热点数据本地缓存，减少 Redis 网络开销 |
| L2 | Redis | 按业务设定 | 分布式缓存，防止缓存雪崩 |

缓存对象包括：歌曲、艺术家、专辑、歌词、外部音乐、主页数据、个人资料。缓存击穿通过 Redisson 分布式锁保护。

### 认证机制

- **用户端**：JWT 双 token（access + refresh），access token 存 sessionStorage/localStorage，请求头 `token` 字段传递
- **Token 版本校验**：Redis 存储每个用户的 token version，登录时更新，实现单设备登录（旧设备自动踢出）
- **CMS 端**：独立的 JWT 和拦截器体系（`AdminTokenInterceptor`）
- **验证码**：AJ-Captcha 滑块拼图，注册和登录时要求验证

### 外部音乐源

系统通过 GD Music API (`music.external.gdmusic`) 接入第三方音源，支持在线搜索和播放网易云等平台的歌曲。外部歌曲播放历史单独记录到 `user_playback_history` 表。

### 路径映射

| URL 路径 | 物理路径 |
|---|---|
| `/spotify-frontend/**` | `classpath:/static/spotify-frontend/dist/` |
| `/spotify-cms/**` | `classpath:/static/spotify-cms/` |
| `/static/datas/**` | 本地磁盘 `static/datas/` 目录 |
| `/uploads/**` | 同上（兼容别名） |

> **注意**：`WebConfig.java` 中 `ResourceHandlerRegistry` 的本地文件路径 (`file:D:/...`) 需要根据实际部署机器修改。

---

## 端口规划

| 服务 | 端口 | 说明 |
|---|---|---|
| 后端主服务 | 5000 | 同时服务用户端和 CMS |
| 用户前端 dev | 8000 | 开发模式独立端口 |
| CMS 前端 dev | 4000 | 开发模式独立端口 |

---

## API 概览

### 用户端（`/spotify/**`）

| 模块 | 路径前缀 | 功能 |
|---|---|---|
| 认证 | `/spotify/login`, `/spotify/signup` | 登录、注册、忘记密码 |
| Token | `/spotify/token/refresh` | 刷新 access token |
| 首页 | `/spotify/home` | 首页推荐内容 |
| 搜索 | `/spotify/search` | 全局搜索 |
| 歌单 | `/spotify/playlist/**` | 歌单详情、收藏、编辑、歌曲增删 |
| 艺术家 | `/spotify/artist/**` | 艺术家详情、关注 |
| 专辑 | `/spotify/album/**` | 专辑详情 |
| 个人资料 | `/spotify/profile/**` | 个人信息、上传歌曲/专辑、我的歌单 |
| 外部音乐 | `/spotify/external/**` | 外部音源搜索 |
| 歌词 | `/spotify/lyrics/**` | 本地/外部歌词 |
| 排行榜 | `/spotify/leaderboard/**` | 全局排行榜 |

### 公开接口（无需登录）

| 路径 | 功能 |
|---|---|
| `/stream/songs/{id}/stream-url` | 获取歌曲播放地址 |
| `/stream/songs/external/{source}/{id}/stream-url` | 获取外部歌曲播放地址 |
| `/captcha/get`, `/captcha/check` | 验证码获取和校验 |

### CMS 端（`/admin/spotify/**`）

| 路径 | 功能 |
|---|---|
| `/admin/spotify/login` | CMS 登录 |
| `/admin/spotify/songs/**` | 歌曲管理 |
| `/admin/spotify/playlists/**` | 歌单管理 |
| `/admin/spotify/artists/**` | 艺术家管理 |

---

## 数据库设计

详细表结构见 `database_spotify.sql`。核心实体关系：

```
User ──┬── 创建 ──→ Playlist ──┬── 包含 ──→ Song
       │                       │
       ├── 收藏 ──→ Playlist    │
       │                       └── song_artist ──→ Artist
       ├── 关注 ──→ Artist
       │               │
       └── 播放历史       └── 属于 ──→ Album
            ↓
       song_play_log / user_playback_history
```

---

## 常见问题

### Q: 后端启动报 Redis 连接失败？

确认 Redis 服务已启动，检查 `application.yaml` 中 `spring.data.redis.host` 和 `port` 是否正确。

### Q: 前端页面白屏？

- 确认前端已执行 `npm run build`（生产模式需要构建产物）
- 开发模式下确认 vite dev server 已启动
- 检查浏览器控制台的 API 请求是否被代理到正确端口

### Q: 音乐播放没有声音？

- 确认 `static/datas/musicResouces/musics/` 目录下有对应的音乐文件
- `WebConfig.java` 中 `ResourceHandlerRegistry` 的本地文件路径是否指向正确的磁盘位置

### Q: 文件上传失败？

- 检查阿里云 OSS 配置是否正确
- 确认 OSS bucket 的读写权限设置
- 检查 `max-file-size` 是否满足需求（默认 50MB）

### Q: CMS 后台无法访问？

CMS 与用户端共用同一个后端实例（端口 5000），访问路径为 `/spotify-cms/`。CMS 的 API 路径为 `/admin/spotify/**`。

---

## 许可证

本项目仅供学习使用。
