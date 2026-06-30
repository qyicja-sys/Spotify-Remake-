const { Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
        Header, Footer, AlignmentType, LevelFormat, HeadingLevel,
        BorderStyle, WidthType, ShadingType, PageNumber, PageBreak,
        TableOfContents } = require('docx');
const fs = require('fs');

const border = { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC" };
const borders = { top: border, bottom: border, left: border, right: border };
const headerBg = { fill: "2E75B6", type: ShadingType.CLEAR };
const cellMargins = { top: 80, bottom: 80, left: 120, right: 120 };

function headerCell(text, width) {
  return new TableCell({
    borders, width: { size: width, type: WidthType.DXA }, shading: headerBg, margins: cellMargins,
    children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text, bold: true, color: "FFFFFF", font: "Arial", size: 22 })] })]
  });
}

function dataCell(text, width) {
  return new TableCell({
    borders, width: { size: width, type: WidthType.DXA }, margins: cellMargins,
    children: [new Paragraph({ children: [new TextRun({ text, font: "Arial", size: 20 })] })]
  });
}

function codeBlock(text) {
  return new Paragraph({
    spacing: { before: 80, after: 80 },
    indent: { left: 360 },
    shading: { fill: "F5F5F5", type: ShadingType.CLEAR },
    children: [new TextRun({ text, font: "Consolas", size: 18, color: "333333" })]
  });
}

const doc = new Document({
  styles: {
    default: { document: { run: { font: "Arial", size: 22 } } },
    paragraphStyles: [
      { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 36, bold: true, font: "Arial", color: "2E75B6" },
        paragraph: { spacing: { before: 360, after: 200 }, outlineLevel: 0 } },
      { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 28, bold: true, font: "Arial", color: "2E75B6" },
        paragraph: { spacing: { before: 280, after: 160 }, outlineLevel: 1 } },
      { id: "Heading3", name: "Heading 3", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 24, bold: true, font: "Arial" },
        paragraph: { spacing: { before: 200, after: 120 }, outlineLevel: 2 } },
    ]
  },
  numbering: {
    config: [
      { reference: "bullets",
        levels: [{ level: 0, format: LevelFormat.BULLET, text: "•", alignment: AlignmentType.LEFT,
          style: { paragraph: { indent: { left: 720, hanging: 360 } } } }] },
      { reference: "numbers",
        levels: [{ level: 0, format: LevelFormat.DECIMAL, text: "%1.", alignment: AlignmentType.LEFT,
          style: { paragraph: { indent: { left: 720, hanging: 360 } } } }] },
    ]
  },
  sections: [{
    properties: {
      page: {
        size: { width: 11906, height: 16838 },
        margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 }
      }
    },
    headers: {
      default: new Header({ children: [new Paragraph({
        alignment: AlignmentType.RIGHT,
        children: [new TextRun({ text: "Spotify Remake — 阿里云OSS文件上传迁移", font: "Arial", size: 18, color: "999999" })]
      })] })
    },
    footers: {
      default: new Footer({ children: [new Paragraph({
        alignment: AlignmentType.CENTER,
        children: [new TextRun({ text: "第 ", font: "Arial", size: 18 }), new TextRun({ children: [PageNumber.CURRENT], font: "Arial", size: 18 })]
      })] })
    },
    children: [
      // ===== 封面 =====
      new Paragraph({ spacing: { before: 3000 }, children: [] }),
      new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 200 },
        children: [new TextRun({ text: "Spotify Remake", font: "Arial", size: 52, bold: true, color: "2E75B6" })] }),
      new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 100 },
        children: [new TextRun({ text: "阿里云 OSS 文件上传迁移", font: "Arial", size: 40, bold: true })] }),
      new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 200 },
        children: [new TextRun({ text: "从本地文件存储到对象存储服务", font: "Arial", size: 26, color: "666666" })] }),
      new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 600 },
        children: [new TextRun({ text: "2026年6月", font: "Arial", size: 22, color: "999999" })] }),

      new Paragraph({ children: [new PageBreak()] }),

      // ===== 目录 =====
      new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun("目录")] }),
      new TableOfContents("目录", { hyperlink: true, headingStyleRange: "1-3" }),

      new Paragraph({ children: [new PageBreak()] }),

      // ===== 1. 背景 =====
      new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun("1. 背景")] }),
      new Paragraph({ spacing: { after: 120 }, children: [new TextRun({ text: "项目原先将用户上传的音乐文件、封面图片、头像等静态资源存储在服务器本地磁盘，通过 Spring Boot 静态资源映射提供访问。随着文件数量增长，本地存储存在以下问题：", font: "Arial", size: 22 })] }),
      new Paragraph({ numbering: { reference: "bullets", level: 0 }, spacing: { after: 60 },
        children: [new TextRun({ text: "磁盘空间受限，音乐文件占用大量存储", font: "Arial", size: 22 })] }),
      new Paragraph({ numbering: { reference: "bullets", level: 0 }, spacing: { after: 60 },
        children: [new TextRun({ text: "部署时需要携带数 GB 静态资源，扩展困难", font: "Arial", size: 22 })] }),
      new Paragraph({ numbering: { reference: "bullets", level: 0 }, spacing: { after: 60 },
        children: [new TextRun({ text: "多实例部署时文件不同步，需要额外的共享存储方案", font: "Arial", size: 22 })] }),
      new Paragraph({ numbering: { reference: "bullets", level: 0 }, spacing: { after: 120 },
        children: [new TextRun({ text: "数据库中存储的是本地相对路径，与服务器文件系统耦合", font: "Arial", size: 22 })] }),

      new Paragraph({ spacing: { after: 120 }, children: [new TextRun({ text: "迁移到阿里云 OSS（Object Storage Service）后，所有文件存储在云端，服务器只处理业务逻辑，实现计算与存储分离。", font: "Arial", size: 22 })] }),

      // ===== 2. 架构设计 =====
      new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun("2. 架构设计")] }),

      new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun("2.1 整体架构")] }),
      new Paragraph({ spacing: { after: 120 }, children: [new TextRun({ text: "迁移采用最小入侵策略：保持 FileUploadUtil 公共方法签名不变，仅替换底层实现。所有 Controller 无需修改，前后端上传流程完全兼容。", font: "Arial", size: 22 })] }),

      new Table({
        width: { size: 9026, type: WidthType.DXA },
        columnWidths: [3000, 3000, 3026],
        rows: [
          new TableRow({ children: [headerCell("组件", 3000), headerCell("作用", 3000), headerCell("文件路径", 3026)] }),
          new TableRow({ children: [
            dataCell("OssConfig", 3000),
            dataCell("配置类，读取 yaml 中 OSS 参数，创建 OSSClient Bean", 3000),
            dataCell("config/OssConfig.java", 3026)
          ]}),
          new TableRow({ children: [
            dataCell("OssInitializer", 3000),
            dataCell("启动类，将 OSS Client 注入到静态工具类 FileUploadUtil", 3000),
            dataCell("config/OssInitializer.java", 3026)
          ]}),
          new TableRow({ children: [
            dataCell("FileUploadUtil", 3000),
            dataCell("文件上传工具类， 7 个公共方法全部改为 OSS 上传", 3000),
            dataCell("utility/FileUploadUtil.java", 3026)
          ]}),
          new TableRow({ children: [
            dataCell("application.yaml", 3000),
            dataCell("OSS 配置：endpoint、bucket、凭据等", 3000),
            dataCell("resources/application.yaml", 3026)
          ]}),
          new TableRow({ children: [
            dataCell("oss_migration.sql", 3000),
            dataCell("数据库迁移脚本，将旧本地路径替换为 OSS URL", 3000),
            dataCell("项目根目录", 3026)
          ]}),
        ]
      }),

      new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun("2.2 OSS 目录结构")] }),
      new Paragraph({ spacing: { after: 80 }, children: [new TextRun({ text: "OSS 上的目录结构与原本地 static/datas/ 目录保持一致，确保新旧数据兼容：", font: "Arial", size: 22 })] }),
      codeBlock("spotify-remake/"),
      codeBlock("├── datas/musicResouces/music_cover/"),
      codeBlock("│   ├── songs/       → 歌曲封面"),
      codeBlock("│   ├── daliy/       → 歌单封面"),
      codeBlock("│   ├── albums/      → 专辑封面"),
      codeBlock("│   └── personal/    → 用户自建歌单封面"),
      codeBlock("├── datas/musicResouces/musics/   → 音乐文件"),
      codeBlock("└── datas/profilePic/"),
      codeBlock("    ├── artists/    → 艺术家头像"),
      codeBlock("    └── *.jpg       → 用户头像"),

      // ===== 3. 实现细节 =====
      new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun("3. 实现细节")] }),

      new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun("3.1 OssConfig — OSS 客户端配置")] }),
      new Paragraph({ spacing: { after: 80 }, children: [new TextRun({ text: "通过 @Value 注解从 application.yaml 读取 OSS 配置参数，创建 OSSClient Bean 供全局使用。", font: "Arial", size: 22 })] }),
      codeBlock("@Configuration"),
      codeBlock("public class OssConfig {"),
      codeBlock("    @Value(\"${aliyun.oss.endpoint}\")"),
      codeBlock("    private String endpoint;      // oss-cn-beijing.aliyuncs.com"),
      codeBlock("    @Value(\"${aliyun.oss.access-key-id}\")"),
      codeBlock("    private String accessKeyId;"),
      codeBlock("    @Value(\"${aliyun.oss.access-key-secret}\")"),
      codeBlock("    private String accessKeySecret;"),
      codeBlock("    @Value(\"${aliyun.oss.bucket-name}\")"),
      codeBlock("    private String bucketName;     // spotify-remake"),
      codeBlock(""),
      codeBlock("    @Bean"),
      codeBlock("    public OSS ossClient() {"),
      codeBlock("        return new OSSClientBuilder()"),
      codeBlock("            .build(endpoint, accessKeyId, accessKeySecret);"),
      codeBlock("    }"),
      codeBlock("}"),

      new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun("3.2 OssInitializer — 静态工具类注入")] }),
      new Paragraph({ spacing: { after: 80 }, children: [new TextRun({ text: "FileUploadUtil 是静态工具类，不受 Spring 管理。OssInitializer 在应用启动时通过 @PostConstruct 将 OSS 客户端、Bucket 名、Endpoint 注入到 FileUploadUtil 的静态字段中。", font: "Arial", size: 22 })] }),

      new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun("3.3 FileUploadUtil — 上传方法")] }),
      new Paragraph({ spacing: { after: 80 }, children: [new TextRun({ text: "7 个公共方法签名保持不变，内部实现从本地 file.transferTo() 改为 ossClient.putObject()。返回值从相对路径改为完整 OSS URL。", font: "Arial", size: 22 })] }),

      new Table({
        width: { size: 9026, type: WidthType.DXA },
        columnWidths: [2200, 2800, 4026],
        rows: [
          new TableRow({ children: [headerCell("方法", 2200), headerCell("用途", 2800), headerCell("OSS Key 前缀", 4026)] }),
          new TableRow({ children: [dataCell("saveCover()", 2200), dataCell("歌曲封面图", 2800), dataCell("datas/musicResouces/music_cover/songs/", 4026)] }),
          new TableRow({ children: [dataCell("saveMusic()", 2200), dataCell("音乐文件", 2800), dataCell("datas/musicResouces/musics/", 4026)] }),
          new TableRow({ children: [dataCell("savePlaylistCover()", 2200), dataCell("歌单封面", 2800), dataCell("datas/musicResouces/music_cover/daliy/", 4026)] }),
          new TableRow({ children: [dataCell("saveAlbumCover()", 2200), dataCell("专辑封面", 2800), dataCell("datas/musicResouces/music_cover/albums/", 4026)] }),
          new TableRow({ children: [dataCell("savePersonalPlaylistCover()", 2200), dataCell("个人歌单封面", 2800), dataCell("datas/musicResouces/music_cover/personal/", 4026)] }),
          new TableRow({ children: [dataCell("saveArtistAvatar()", 2200), dataCell("艺术家头像", 2800), dataCell("datas/profilePic/artists/ + users/", 4026)] }),
          new TableRow({ children: [dataCell("saveUserProfilePic()", 2200), dataCell("用户头像", 2800), dataCell("datas/profilePic/", 4026)] }),
        ]
      }),

      new Paragraph({ spacing: { before: 120 }, children: [] }),

      new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun("3.4 URL 生成逻辑")] }),
      new Paragraph({ spacing: { after: 80 }, children: [new TextRun({ text: "buildOssUrl() 方法动态拼接完整 OSS 访问 URL：", font: "Arial", size: 22 })] }),
      codeBlock("private static String buildOssUrl(String objectKey) {"),
      codeBlock("    String host = endpoint.replace(\"https://\", \"\")"),
      codeBlock("                         .replace(\"http://\", \"\");"),
      codeBlock("    return \"https://\" + bucketName + \".\" + host + \"/\" + objectKey;"),
      codeBlock("}"),
      codeBlock("// 生成示例："),
      codeBlock("// https://spotify-remake.oss-cn-beijing.aliyuncs.com/datas/musicResouces/musics/周杰伦 - 七里香.mp3"),

      new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun("3.5 头像 OSS 内复制")] }),
      new Paragraph({ spacing: { after: 80 }, children: [new TextRun({ text: "艺术家注册时，需要将用户头像复制到艺术家目录。新增 copyAvatarToArtistDir() 方法，通过 ossClient.copyObject() 在 OSS 内部复制对象，避免下载再上传。", font: "Arial", size: 22 })] }),

      // ===== 4. 数据库迁移 =====
      new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun("4. 数据库迁移")] }),
      new Paragraph({ spacing: { after: 80 }, children: [new TextRun({ text: "旧数据中存储的是本地相对路径（如 /static/datas/musicResouces/musics/xxx.mp3），需要替换为 OSS 完整 URL。迁移 SQL 影响 5 张表、7 个字段：", font: "Arial", size: 22 })] }),

      new Table({
        width: { size: 9026, type: WidthType.DXA },
        columnWidths: [1800, 2500, 2000, 2726],
        rows: [
          new TableRow({ children: [headerCell("表名", 1800), headerCell("字段", 2500), headerCell("旧值示例", 2000), headerCell("新值示例", 2726)] }),
          new TableRow({ children: [
            dataCell("songs", 1800),
            dataCell("coverUrl, fileUrl", 2500),
            dataCell("/static/datas/...", 2000),
            dataCell("https://spotify-remake.oss-cn-beijing.aliyuncs.com/datas/...", 2726)
          ]}),
          new TableRow({ children: [
            dataCell("artists", 1800),
            dataCell("avatarUrl", 2500),
            dataCell("/static/datas/...", 2000),
            dataCell("https://spotify-remake.oss-cn-beijing.aliyuncs.com/datas/...", 2726)
          ]}),
          new TableRow({ children: [
            dataCell("playlists", 1800),
            dataCell("coverUrl, backgroundUrl", 2500),
            dataCell("/static/datas/...", 2000),
            dataCell("https://spotify-remake.oss-cn-beijing.aliyuncs.com/datas/...", 2726)
          ]}),
          new TableRow({ children: [
            dataCell("albums", 1800),
            dataCell("coverUrl", 2500),
            dataCell("/static/datas/...", 2000),
            dataCell("https://spotify-remake.oss-cn-beijing.aliyuncs.com/datas/...", 2726)
          ]}),
          new TableRow({ children: [
            dataCell("user", 1800),
            dataCell("profilePic", 2500),
            dataCell("/static/datas/...", 2000),
            dataCell("https://spotify-remake.oss-cn-beijing.aliyuncs.com/datas/...", 2726)
          ]}),
        ]
      }),

      new Paragraph({ spacing: { before: 120 }, children: [] }),
      new Paragraph({ spacing: { after: 80 }, children: [new TextRun({ text: "迁移原理：将 /static/ 前缀替换为 OSS 完整域名，保留后缀路径不变。只更新 LIKE '/static/%' 的记录，网络 URL（coverNetworkUrl 等）不受影响。", font: "Arial", size: 22 })] }),
      codeBlock("SET @oss_base = 'https://spotify-remake.oss-cn-beijing.aliyuncs.com';"),
      codeBlock("UPDATE songs SET coverUrl = CONCAT(@oss_base, '/',"),
      codeBlock("    SUBSTRING(coverUrl, 9)) WHERE coverUrl LIKE '/static/%';"),

      // ===== 5. 前端兼容性 =====
      new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun("5. 前端兼容性")] }),
      new Paragraph({ spacing: { after: 120 }, children: [new TextRun({ text: "前端不需要任何修改。原因：", font: "Arial", size: 22 })] }),
      new Paragraph({ numbering: { reference: "bullets", level: 0 }, spacing: { after: 60 },
        children: [new TextRun({ text: "所有文件 URL 都是从后端 API 响应中拿到的，前端不构造文件路径", font: "Arial", size: 22 })] }),
      new Paragraph({ numbering: { reference: "bullets", level: 0 }, spacing: { after: 60 },
        children: [new TextRun({ text: "resolveUrl() 函数检测 http/https 前缀，OSS URL 原样返回，浏览器直接从阿里云加载", font: "Arial", size: 22 })] }),
      new Paragraph({ numbering: { reference: "bullets", level: 0 }, spacing: { after: 60 },
        children: [new TextRun({ text: "imgUrl(coverUrl, coverNetworkUrl) 回路模式优先使用网络 URL，与 OSS URL 完美兼容", font: "Arial", size: 22 })] }),
      new Paragraph({ numbering: { reference: "bullets", level: 0 }, spacing: { after: 60 },
        children: [new TextRun({ text: "上传端点路径未变，FormData 构建逻辑不变", font: "Arial", size: 22 })] }),

      new Paragraph({ spacing: { before: 120 }, children: [] }),
      new Paragraph({ spacing: { after: 80 }, children: [new TextRun({ text: "缓存问题修复：", font: "Arial", size: 22, bold: true })] }),
      new Paragraph({ spacing: { after: 120 }, children: [new TextRun({ text: "OSS 同名覆盖文件时 URL 不变，浏览器会使用缓存旧图。在 openProfile()、refreshDashboard()、saveEditProfile() 中统一添加 ?t= 时间戳参数，确保上传后立即显示新图片。", font: "Arial", size: 22 })] }),

      // ===== 6. 控制器调用关系 =====
      new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun("6. 控制器调用关系")] }),
      new Paragraph({ spacing: { after: 80 }, children: [new TextRun({ text: "以下 8 个控制器端点调用 FileUploadUtil，此次迁移中均未修改：", font: "Arial", size: 22 })] }),

      new Table({
        width: { size: 9026, type: WidthType.DXA },
        columnWidths: [3000, 3000, 3026],
        rows: [
          new TableRow({ children: [headerCell("控制器", 3000), headerCell("端点", 3000), headerCell("调用方法", 3026)] }),
          new TableRow({ children: [dataCell("AdminSongController", 3000), dataCell("POST /admin/spotify/songs/upload", 3000), dataCell("saveCover + saveMusic", 3026)] }),
          new TableRow({ children: [dataCell("AdminAlbumController", 3000), dataCell("POST /admin/spotify/albums/upload-cover", 3000), dataCell("saveAlbumCover", 3026)] }),
          new TableRow({ children: [dataCell("AdminPlaylistController", 3000), dataCell("POST /admin/spotify/playlists/upload-cover", 3000), dataCell("savePlaylistCover", 3026)] }),
          new TableRow({ children: [dataCell("ArtistController", 3000), dataCell("POST /admin/spotify/artists/upload-avatar", 3000), dataCell("saveArtistAvatar", 3026)] }),
          new TableRow({ children: [dataCell("mainWebController", 3000), dataCell("POST /spotify/profile/avatar", 3000), dataCell("saveUserProfilePic", 3026)] }),
          new TableRow({ children: [dataCell("mainWebController", 3000), dataCell("POST /spotify/profile/upload-song", 3000), dataCell("saveCover + saveMusic", 3026)] }),
          new TableRow({ children: [dataCell("UserAlbumController", 3000), dataCell("POST /spotify/profile/create-album", 3000), dataCell("saveAlbumCover + saveMusic", 3026)] }),
          new TableRow({ children: [dataCell("UserPlaylistController", 3000), dataCell("POST /spotify/playlist/{id}/edit", 3000), dataCell("savePersonalPlaylistCover", 3026)] }),
        ]
      }),

      // ===== 7. 配置说明 =====
      new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun("7. 配置说明")] }),
      new Paragraph({ spacing: { after: 80 }, children: [new TextRun({ text: "application.yaml 中 OSS 配置段：", font: "Arial", size: 22 })] }),
      codeBlock("aliyun:"),
      codeBlock("  oss:"),
      codeBlock("    endpoint: https://oss-cn-beijing.aliyuncs.com"),
      codeBlock("    access-key-id: <你的 AccessKey ID>"),
      codeBlock("    access-key-secret: <你的 AccessKey Secret>"),
      codeBlock("    bucket-name: spotify-remake"),
      codeBlock("    region: cn-beijing"),

      new Paragraph({ spacing: { before: 120 }, children: [] }),
      new Paragraph({ spacing: { after: 80 }, children: [new TextRun({ text: "pom.xml 中 OSS SDK 依赖：", font: "Arial", size: 22 })] }),
      codeBlock("<dependency>"),
      codeBlock("    <groupId>com.aliyun.oss</groupId>"),
      codeBlock("    <artifactId>aliyun-sdk-oss</artifactId>"),
      codeBlock("    <version>3.17.4</version>"),
      codeBlock("</dependency>"),

      // ===== 8. 本地文件上传到OSS =====
      new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun("8. 本地文件上传到 OSS")] }),

      new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun("8.1 方式一：OSS 控制台")] }),
      new Paragraph({ spacing: { after: 80 }, children: [new TextRun({ text: "登录阿里云 OSS 控制台 → 进入 spotify-remake Bucket → 拖拽文件上传。适合少量文件。", font: "Arial", size: 22 })] }),

      new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun("8.2 方式二：ossutil 命令行")] }),
      new Paragraph({ spacing: { after: 80 }, children: [new TextRun({ text: "安装 ossutil 工具后，批量上传：", font: "Arial", size: 22 })] }),
      codeBlock("# 配置凭据"),
      codeBlock("ossutil config -e oss-cn-beijing.aliyuncs.com \\"),
      codeBlock("    -i $OSS_ACCESS_KEY_ID -k $OSS_ACCESS_KEY_SECRET"),
      codeBlock(""),
      codeBlock("# 批量上传音乐文件"),
      codeBlock("ossutil cp -r musics/ oss://spotify-remake/datas/musicResouces/musics/"),
      codeBlock(""),
      codeBlock("# 批量上传封面"),
      codeBlock("ossutil cp -r music_cover/ oss://spotify-remake/datas/musicResouces/music_cover/"),
      codeBlock(""),
      codeBlock("# 批量上传头像"),
      codeBlock("ossutil cp -r profilePic/ oss://spotify-remake/datas/profilePic/"),

      // ===== 9. 技术要点 =====
      new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun("9. 技术要点")] }),

      new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun("9.1 最小入侵")] }),
      new Paragraph({ spacing: { after: 80 }, children: [new TextRun({ text: "FileUploadUtil 公共 API 完全不变，8 个 Controller 零修改。只替换了底层存储层，业务逻辑不受影响。", font: "Arial", size: 22 })] }),

      new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun("9.2 OSS 内复制")] }),
      new Paragraph({ spacing: { after: 80 }, children: [new TextRun({ text: "艺术家注册时的头像复制使用 ossClient.copyObject()，在 OSS 服务器端完成，不经过应用服务器中转，几乎零延迟。", font: "Arial", size: 22 })] }),

      new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun("9.3 浏览器缓存处理")] }),
      new Paragraph({ spacing: { after: 80 }, children: [new TextRun({ text: "OSS 同名覆盖文件时 URL 不变，需要在前端添加 ?t= 查询参数做缓存破坏。已在 openProfile()、refreshDashboard()、saveEditProfile() 三处统一处理。", font: "Arial", size: 22 })] }),

      new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun("9.4 配置安全")] }),
      new Paragraph({ spacing: { after: 80 }, children: [new TextRun({ text: "application.yaml 已加入 .gitignore，确保 AccessKey 不会提交到 Git 仓库。生产环境应使用 RAM 角色 + 环境变量。", font: "Arial", size: 22 })] }),

      // ===== 10. 文件清单 =====
      new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun("10. 文件清单")] }),

      new Table({
        width: { size: 9026, type: WidthType.DXA },
        columnWidths: [2500, 1500, 5026],
        rows: [
          new TableRow({ children: [headerCell("文件", 2500), headerCell("操作", 1500), headerCell("说明", 5026)] }),
          new TableRow({ children: [dataCell("config/OssConfig.java", 2500), dataCell("新建", 1500), dataCell("OSS 客户端配置类", 5026)] }),
          new TableRow({ children: [dataCell("config/OssInitializer.java", 2500), dataCell("新建", 1500), dataCell("静态工具类注入器", 5026)] }),
          new TableRow({ children: [dataCell("utility/FileUploadUtil.java", 2500), dataCell("重写", 1500), dataCell("所有方法改为 OSS 上传", 5026)] }),
          new TableRow({ children: [dataCell("application.yaml", 2500), dataCell("修改", 1500), dataCell("新增 OSS 配置段", 5026)] }),
          new TableRow({ children: [dataCell(".gitignore", 2500), dataCell("修改", 1500), dataCell("屏蔽 application.yaml", 5026)] }),
          new TableRow({ children: [dataCell("oss_migration.sql", 2500), dataCell("新建", 1500), dataCell("数据库迁移脚本", 5026)] }),
          new TableRow({ children: [dataCell("ProfileServiceiml.java", 2500), dataCell("修改", 1500), dataCell("头像复制改为 OSS 内复制", 5026)] }),
          new TableRow({ children: [dataCell("ArtistController.java", 2500), dataCell("修改", 1500), dataCell("头像复制改为 OSS 内复制", 5026)] }),
        ]
      }),
    ]
  }]
});

Packer.toBuffer(doc).then(buffer => {
  const path = "D:/javaedit/project/spotify/Spotify_remake/Spotify_Remake/项目亮点/阿里云OSS文件上传迁移.docx";
  fs.writeFileSync(path, buffer);
  console.log("DOCX created:", path);
});
