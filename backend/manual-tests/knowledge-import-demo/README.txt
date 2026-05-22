管理端知识库批量导入演示数据

说明：
1. 本目录下的 .txt 文件内容均为 UTF-8 编码的 JSON 请求体。
2. 字段结构与管理端批量导入接口一致，可直接复制到 Swagger 或 Postman 的请求体中使用。
3. 如果你想把文件临时改成 .json 再导入，也可以，内容本身已经是合法 JSON。

对应接口：
1. 面料导入：POST /api/v1/admin/knowledge/fabrics/import
2. 指南导入：POST /api/v1/admin/knowledge/guides/import
3. 洗护标签导入：POST /api/v1/admin/knowledge/care-labels/import

建议演示方式：
1. 先打开 txt 文件展示“准备好的知识数据”
2. 将内容复制到接口调试工具中发送
3. 返回成功后刷新管理端知识库列表
4. 再到用户端或知识问答页面验证新增内容是否可检索

文件列表：
1. fabrics-demo-import.txt
2. guides-demo-import.txt
3. care-labels-demo-import.txt
