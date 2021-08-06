# DBeaver 虚谷数据库插件

此插件提供了 DBeaver 客户端对虚谷数据库的支持。

## 部署说明

### 下载安装 DBeaver 7.0.2

下载地址：[https://github.com/dbeaver/dbeaver/releases](https://github.com/dbeaver/dbeaver/releases)

### 注册插件至 DBeaver 7.0.2

1. 将插件 jar 包放入 `DBeaver（安装目录）/plugins` 目录中
2. 使用文本编辑器打开 `DBeaver（安装目录）/configuration/org.eclipse.equinox.simpleconfigurator/bundles.info` 文件
3. 在文件末尾按照配置格式新增一行配置项，配置格式为：  
`org.jkiss.dbeaver.ext.xugu,{版本号},plugins/org.jkiss.dbeaver.ext.xugu_{版本号}.jar,4,false`

## 目录结构

### 根目录

路径|说明
---|---
src|源码目录
.settings|Eclipse 项目配置目录
bin|
icons|图标目录
lib|依赖包目录
META-INF|元数据配置目录
OSGI-INF|国际化配置目录

### 源码目录（org.jkiss.dbeaver.ext.xugu）

路径|说明
---|---
actions|任务类目录（默认oracle实现）
data|数据类型转化类目录（默认oracle实现）
edit|数据库各个实体的管理类目录，部分管理类包含有内部类界面（自主实现）
editors|额外的编辑器界面类目录（自主实现）
model|数据库各个实体类目录（自主实现）
views|额外的页面类目录（自主实现）

## 开发说明

### 环境要求

- JDK 8
- Eclipse IDE for RCP and RAP Developers
- Apache Maven 3
- Git
- DBeaver 7.0.2

### 安装依赖插件

1. 菜单栏 -> 帮助 -> 安装新软件
2. 填入地址并回车：  
<http://dbeaver.io/eclipse-repo>
3. 勾选所有插件并安装（暂不重启 Eclipse）
5. 填入地址并回车：  
<http://eclipse-color-theme.github.com/update>
6. 勾选所有插件并安装
7. 填入地址并回车：  
<http://download.eclipse.org/releases/latest>
8. 搜索`Mylyn Context Connector: Plug-in Development`，勾选插件并安装
9. 重启 Eclipse

### Eclipse 导入 DBeaver 项目

1. 菜单栏 -> 文件 -> 导入
2. Maven -> 已存在的 Maven 项目
3. 选择 DBeaver 项目目录
4. 勾选所有项目模块
5. 导入项目
6. 执行一次`mvn clean`以处理依赖包

### Eclipse 导入启动配置

1. 菜单栏 -> 文件 -> 导入
2. 运行/调试 -> 启动配置
3. 选择 DBeaver/product/debug 目录
4. 勾选所有启动配置文件
5. 导入启动配置

### 导入必需插件

1. 克隆本项目至 DBeaver/plugins 目录
1. 菜单栏 -> 运行 -> 运行配置
2. 选择 DBeaver.product 项
3. 选择 Plug-ins 选项卡
4. 勾选 org.jkiss.dbeaver.ext.xugu 插件
5. 点击”添加需要的插件（Add Required Plug-ins）”按钮
6. 应用更改
7. 点击运行/调试

## OEM 替换指导

1. 使用 Eclipse 正确打开整个项目与虚谷插件项目
2. 在资源管理器中，复制虚谷插件项目目录到同层并修改目录名为 OEM 目录名
3. 使用 Eclipse 刷新目录树
4. 复制 icons 目录下对应 OEM 目录下所有文件到 icons 目录替换相关图片
4. 复制 lib 目录下对应 OEM 目录下所有文件到 lib 目录替换默认驱动
4. 在 OEM 目录的 src 目录右键-properties，填写编码为 GB18030，应用并关闭
5. 修改 src/org/jkiss/dbeaver/ext/xugu 目录名称为对应的 OEM 名称
5. 使用 Eclipse 点击 OEM 目录，按下快捷键 Ctrl+H 打开文件搜索，开启大小写敏感（Case sensitive），选择搜索范围为 Selected resource in 'Project Explorer'，替换所有 ext.xugu 为对应 OEM 名称
6. 使用 Eclipse 点击 OEM 目录，按下快捷键 Ctrl+H 打开文件搜索，开启大小写敏感（Case sensitive），选择搜索范围为 Selected resource in 'Project Explorer'，搜索所有 Xugu，排除以下目录，然后替换所有为对应 OEM 名称
    - src/org/jkiss/dbeaver/ext/{OEM}/model 目录
7. 使用 Eclipse 点击 OEM 目录，按下快捷键 Ctrl+H 打开文件搜索，开启大小写敏感（Case sensitive），选择搜索范围为 Selected resource in 'Project Explorer'，搜索所有 xugu，仅替换以下目录文件内容为对应 OEM 名称
    - plugin.xml 文件
8. 使用 Eclipse 点击 OEM 目录，右键-Import as Project 导入 OEM 项目
9. 替换完成，可在启动配置中添加 OEM 插件进行 debug 以及导出 OEM 插件包

## 打包需要导出的插件

- org.jkiss.dbeaver.data.office
- org.jkiss.dbeaver.data.transfer
- org.jkiss.dbeaver.ext.oracle
- org.jkiss.dbeaver.ext.{OEM}
- org.jkiss.dbeaver.model
- org.jkiss.dbeaver.registry
- org.jkiss.dbeaver.ui.editors.sql
- org.jkiss.dbeaver.ui.navigator
- org.jkiss.dbeaver.ui
