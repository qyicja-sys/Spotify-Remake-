## 关于我

* [ty1l / 软件工程师 / 学生]。
* 我用 Codex 做 [软件、web开发] 和 [项目管理]。

## 思维原则
* 避免重复。
* 所有决策从问题本质出发，不因「惯例如此」照搬。
* 回到问题本身：要解决什么？最直接的路径是什么？从零设计会怎么做？
* 不要谄媚。不要夸我的想法好、不要说「这是一个很好的问题」、不要开头说废话。给我真实判断，方案有问题直接指出。发现更好的做法直接说，不用征求意见。

## 行为准则

1. 以暗猜接口为耻，以认真查阅为荣
2. 以模糊执行为耻，以寻求确认为荣
3. 以盲想业务为耻，以人类确认为荣
4. 以创造接口为耻，以复用现有为荣
5. 以跳过验证为耻，以主动测试为荣
6. 以破坏架构为耻，以遵循规范为荣
7. 以假装理解为耻，以诚实无知为荣
8. 以盲目修改为耻，以谨慎重构为荣

## 约束先行

* 无论开发项目还是知识管理项目，第一步永远是建规则：新项目先读取或写 `AGENTS.md` 规范。
* 没有规范的工作空间不动手。已有规范的项目，严格遵守其 `AGENTS.md` 中的所有约定。

## 沟通方式

* 默认中文，代码、命令、变量名用英文。
* 结论先行，再给理由，不要先铺垫背景。
* 遇到模糊需求，先给最合理的方案，再问要不要调整 - 不要问「你具体想要什么」。

## 自主边界（红线，必须先问我）

以下操作即使在 `auto-accept` 模式下也必须停下来问我：

* 删除文件、目录或 git 历史
* 修改 `.env`、密钥、token、CI/CD 配置
* 数据库 schema 变更或数据迁移
* `git push`、`git rebase`、`git reset --hard`、强制推送
* 安装新的全局依赖或修改系统配置
* 公开发布（`npm publish`、部署到生产、发文章等）

## 通用工程纪律

* 改完主动跑验证（具体命令见各项目 `AGENTS.md`），不要只改不验证。
* 不要为了让代码跑起来注释掉报错或加绕过标记，找根本原因。
* 密钥、token、密码不进代码、不进 commit、不进日志 - 大改动前必须确认 `.gitignore` 状态。
* 每次修改完代码后，必须跑验证，确认代码是否正常运行(使用的数据库为database_spotify)
* 写新功能时，必须先查看能否写在已有的服务类中，不能则创建新的服务类。

## 工程

### 概述
Spotify 重制项目，Spring Boot + Vue 3 前后端分离架构。

### 技术栈
- **后端**: Spring Boot 3.2.0, Java 17, MyBatis, MySQL, Spring Security, JWT, AJ-Captcha
- **前端**: Vue 3 + Vite (客户端 + CMS 两套独立前端)
- **构建**: Maven

### 后端结构 (`src/main/java/com/ty1l/spotify_remake/`)

```
├── SpotifyRemakeApplication.java    # 启动类
├── config/                          # 配置类 (SecurityConfig, WebConfig, CaptchaConfig)
├── interceptor/                     # 拦截器 (TokenInterceptor, AdminTokenInterceptor, SignedUrlFilter)
├── Controller/
│   ├── User/        # 用户端接口 (登录/注册/歌单/主页)
│   ├── Admin/       # CMS端接口 (管理登录/歌单/歌曲)
│   └── Public/      # 公开接口 (搜索/歌曲/歌单/艺术家/外部音乐/歌曲流)
├── Service/
│   ├── User/        # 用户服务 (含实现类 *Serviceiml)
│   ├── Admin/       # CMS服务
│   └── Public/      # 公共服务 (含 External/ 子包 — GDMusicApiClient)
├── Entity/
│   ├── User/        # 用户实体 & VO (User, Playlist, LoginInfoVo, ProfileVO 等)
│   ├── Admin/       # 管理员实体 (Admin, AdminPlaylist)
│   └── Public/      # 公共实体 (Song, Artist, SearchVo 等)
├── Mapper/
│   ├── User/        # 用户 Mapper 接口
│   ├── Admin/       # 管理员 Mapper 接口
│   └── Public/      # 公共 Mapper 接口
├── Exception/       # 全局异常处理 (GlobalExceptionHandler)
└── utility/         # 工具类 (JwtGenerate, Result, FileUploadUtil, BaseContext 等)
```

**注意**: 包名大小写不一致是现状（`Controller` 大写 C，`mapper`/`service` 小写），不要批量重命名。

### MyBatis XML Mapper
位于 `src/main/resources/com/ty1l/spotify_remake/Mapper/*.xml`，与 Java Mapper 接口同包名但不同根路径。

### 前端结构

**用户前端** (`src/main/resources/static/spotify-frontend/`):
- Vue 3 + Vite 项目，源码在 `src/`，构建产物在 `dist/`
- 组件: `AuthPage.vue`, `MainApp.vue`, `CaptchaModal.vue` 等
- 样式: `src/CSS/auth-page.css`, `src/CSS/main-layout.css`

**CMS 前端** (`src/main/resources/static/spotify-cms/`):
- 独立 Vue 3 + Vite 项目
- 视图: `LoginView`, `DashboardView`, `SongsView`, `PlaylistsView`, `ArtistsView`
- UI 组件库: `src/components/ui/` (Button, Dialog, Table, Input 等)

### 静态资源 (`src/main/resources/static/datas/`)
- `musicResouces/musics/` — 音乐文件
- `musicResouces/music_cover/` — 封面图
- `profilePic/` — 用户头像
- `profilePic/artists/` — 艺术家头像

### 数据库文档
- `database_spotify_user.txt` — 用户端数据库表结构
- `database_spotify_admin.txt` — CMS端数据库表结构
- `GD_Music_Api.txt` — 外部音乐 API 文档

### 配置文件
- `application.yaml` — 主配置（数据库连接、MyBatis、验证码、外部API）
- 数据库: `database_spotify` (MySQL)
- 用户端端口: 5000, CMS端端口: 4000

### 根目录其他文件
- `pom.xml` — Maven 配置
- `项目痛点/` — 项目问题记录
- `Codex history/` — Codex 对话历史
- `error log/` — 错误日志

## 前端样式规范

### 歌曲列表模板（song-table）
所有需要展示歌曲列表的页面（歌单详情页、艺术家详情页、个人资料页等），**必须使用统一的 song-table 模板结构**，不要自创样式。

**模板结构（MainApp.vue 中歌单详情页的 song-table 为标准参考）：**

表头列：`#` | `标题` | `专辑` | (收藏按钮) | (时钟图标) | (更多)

每行（`.song-row`）包含以下列，顺序不可变：
1. `.song-col-num` — 序号，悬浮显示播放三角图标（`.song-play-icon`），使用 16x16 Spotify 标准三角 SVG
2. `.song-col-title` — `.song-thumb`（40x40 封面，直接在 `<img>` 上）+ `.song-title-info`（歌名 + 艺术家名）
3. `.song-col-album` — 专辑名，无数据时填 `—`
4. `.song-col-add` — 收藏按钮，`.song-action-btn` + `.liked` 状态（绿色圆勾），点击调用 `handleLikeSong`
5. `.song-col-duration` — 时长，使用 `formatDuration()` 函数，时钟图标使用 16x16 标准 SVG
6. `.song-col-more` — 更多按钮（三点图标），使用 16x16 标准 SVG

**交互：**
- 整行点击 `playSong(song, queue)`
- 序号列 `@click.stop` 阻止冒泡后直接播放
- 收藏/更多按钮 `@click.stop` 阻止冒泡

**CSS 类名（main-layout.css）：** `.song-table`, `.song-table-header`, `.song-row`, `.song-col-num`, `.song-col-title`, `.song-thumb`, `.song-title-info`, `.song-title-text`, `.song-artist-text`, `.song-col-album`, `.song-col-add`, `.song-action-btn`, `.song-col-duration`, `.song-col-more`, `.song-play-icon`, `.song-num-text`

**注意：** 不要省略列、不要改变列顺序、不要自创图标。参考 `MainApp.vue` 中 `playlistDetail.songs` 的渲染部分作为唯一模板。

### 歌单/作者详情页布局
歌单详情页（playlist-detail）和未来的作者详情页，统一使用以下布局规范：

**整体结构：**
- 背景区域（playlist-bg-section）+ 内容区域（playlist-content-section）
- 背景区域使用封面图或渐变色作为背景
- 磨砂渐变边界效果：`playlist-bg-blur` 层，`backdrop-filter: blur`，40% 处硬切到 `#121212`，形成明显边界感

**头部信息（playlist-header）：**
- 纵向排列（flex-direction: column），信息在封面图下方
- 顺序：封面图 → 标题 → 简介(profile) → 品牌Logo+名称 → 歌曲数•总时长
- 封面图 232px，圆角 4px，阴影 `0 4px 60px rgba(0,0,0,0.5)`

**操作按钮（playlist-actions）：**
- 位于渐变层之上（z-index: 5），不被磨砂效果遮盖
- 按钮顺序：播放（绿色圆形）→ 分享 → 添加 → 下载 → 更多
- 按钮图标使用 SVG，颜色 `#b3b3b3`，hover 变白

**关键 CSS 类名：**
- `.playlist-detail` — 最外层容器
- `.playlist-bg-section` — 背景区域
- `.playlist-bg-blur` — 磨砂渐变层
- `.playlist-header` — 头部信息（纵向布局）
- `.playlist-cover` — 封面图
- `.playlist-info` — 文字信息容器
- `.playlist-title` — 标题（48px, 900 weight）
- `.playlist-profile` — 简介（14px, #b3b3b3）
- `.playlist-brand` — 品牌区域（Logo + 名称）
- `.playlist-meta` — 元信息（歌曲数、时长）
- `.playlist-actions` — 操作按钮区域
- `.action-icon-btn` — 功能按钮（分享/添加/下载/更多）

## 验证与调试
* **后端需要用户手动启动**，Codex 无法自动编译运行（环境只有 JRE 没有 JDK）。
* 每次要用 agent-browser 打开前端页面验证结果前，**必须先提醒用户启动后端**，等用户确认后再继续。
* 需要验证的网址有：客户端——http://localhost:5000/  ；  cms——http://localhost:4000/
* 使用agent-browser来验证项目，登录界面后的主界面需要的邮箱密码我给你提供一套，邮箱：3531185223@qq.com，密码：123456