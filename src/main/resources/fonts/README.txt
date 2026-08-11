请将开源中文字体文件改名为 chinese.ttf 放入此目录。

推荐字体（均为开源可商用）:
- 阿里巴巴普惠体: https://alibabafont.taobao.com/
- 思源黑体 (Source Han Sans): https://github.com/adobe-fonts/source-han-sans
- 霞鹜文楷 (LXGW WenKai): https://github.com/lxgw/LxgwWenKai

备选方案（无需内置字体）:
在 CentOS 服务器上执行:
  yum install -y google-noto-sans-cjk-sc-fonts
  fc-cache -fv
