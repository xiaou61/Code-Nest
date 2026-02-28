FROM criyle/go-judge:latest

USER root

# 安装编译器: Java, GCC, G++, Python3, Go, Node.js
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        default-jdk \
        gcc \
        g++ \
        python3 \
        golang \
        nodejs && \
    rm -rf /var/lib/apt/lists/*

# 验证编译器安装
RUN javac -version && gcc --version && g++ --version && python3 --version && go version && node --version
