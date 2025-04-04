// 弹幕系统客户端示例

// 1. 获取连接信息
async function getConnectionInfo(roomId, userId = null) {
  try {
    // 构建请求URL
    let url = `/api/v1/connect?roomId=${roomId}`;
    if (userId) {
      url += `&userId=${userId}`;
    }

    // 发送请求
    const response = await fetch(url);
    if (!response.ok) {
      throw new Error(`HTTP错误 ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error('获取连接信息失败:', error);
    throw error;
  }
}

// 2. 建立WebSocket连接
class BarrageClient {
  constructor(roomId, userId = null) {
    this.roomId = roomId;
    this.userId = userId;

    this.heartbeatWs = null;
    this.danmakuWs = null;
    this.cdnWs = null;

    this.heartbeatInterval = null;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectDelay = 2000;

    this.onDanmakuCallbacks = [];
    this.onUserJoinCallbacks = [];
    this.onUserLeaveCallbacks = [];
    this.onCdnInfoCallbacks = [];
  }

  // 连接到弹幕系统
  async connect() {
    try {
      // 获取连接信息
      const result = await getConnectionInfo(this.roomId, this.userId);

      if (result.code !== 0 || !result.data) {
        throw new Error(`获取连接信息失败: ${result.message}`);
      }

      const connectionInfo = result.data;

      // 建立心跳连接
      this.connectHeartbeat(connectionInfo.serverInfo.heartbeatUrl);

      // 建立弹幕连接
      this.connectDanmaku(connectionInfo.serverInfo.danmakuUrl);

      // 建立CDN信息连接
      this.connectCdn(connectionInfo.serverInfo.cdnUrl);

      return true;
    } catch (error) {
      console.error('连接弹幕系统失败:', error);
      this.cleanup();
      return false;
    }
  }

  // 建立心跳连接
  connectHeartbeat(url) {
    this.heartbeatWs = new WebSocket(url);

    this.heartbeatWs.onopen = () => {
      console.log('心跳连接已建立');

      // 开始发送心跳
      this.heartbeatInterval = setInterval(() => {
        if (this.heartbeatWs && this.heartbeatWs.readyState
            === WebSocket.OPEN) {
          this.heartbeatWs.send(JSON.stringify({type: 'HEARTBEAT'}));
        }
      }, 20000); // 每20秒发送一次心跳
    };

    this.heartbeatWs.onmessage = (event) => {
      const data = JSON.parse(event.data);
      if (data.type === 'HEARTBEAT') {
        console.log('收到心跳响应:', data.data.timestamp);
      }
    };

    this.heartbeatWs.onerror = (error) => {
      console.error('心跳连接错误:', error);
    };

    this.heartbeatWs.onclose = (event) => {
      console.log('心跳连接已关闭:', event.code, event.reason);

      // 清理心跳定时器
      if (this.heartbeatInterval) {
        clearInterval(this.heartbeatInterval);
        this.heartbeatInterval = null;
      }

      // 尝试重连
      this.attemptReconnect('heartbeat');
    };
  }

  // 建立弹幕连接
  connectDanmaku(url) {
    this.danmakuWs = new WebSocket(url);

    this.danmakuWs.onopen = () => {
      console.log('弹幕连接已建立');
      this.reconnectAttempts = 0; // 连接成功后重置重连次数
    };

    this.danmakuWs.onmessage = (event) => {
      const data = JSON.parse(event.data);

      switch (data.type) {
        case 'DANMAKU':
          // 触发弹幕回调
          this.onDanmakuCallbacks.forEach(callback => callback(data));
          break;
        case 'JOIN':
          // 触发用户加入回调
          this.onUserJoinCallbacks.forEach(callback => callback(data));
          break;
        case 'LEAVE':
          // 触发用户离开回调
          this.onUserLeaveCallbacks.forEach(callback => callback(data));
          break;
      }
    };

    this.danmakuWs.onerror = (error) => {
      console.error('弹幕连接错误:', error);
    };

    this.danmakuWs.onclose = (event) => {
      console.log('弹幕连接已关闭:', event.code, event.reason);

      // 尝试重连
      this.attemptReconnect('danmaku');
    };
  }

  // 建立CDN信息连接
  connectCdn(url) {
    this.cdnWs = new WebSocket(url);

    this.cdnWs.onopen = () => {
      console.log('CDN信息连接已建立');
    };

    this.cdnWs.onmessage = (event) => {
      const data = JSON.parse(event.data);

      if (data.type === 'CDN_INFO') {
        // 触发CDN信息回调
        this.onCdnInfoCallbacks.forEach(callback => callback(data));
      }
    };

    this.cdnWs.onerror = (error) => {
      console.error('CDN信息连接错误:', error);
    };

    this.cdnWs.onclose = (event) => {
      console.log('CDN信息连接已关闭:', event.code, event.reason);

      // 尝试重连
      this.attemptReconnect('cdn');
    };
  }

  // 发送弹幕
  sendDanmaku(content, color = '#FFFFFF', fontSize = 24, mode = 1) {
    if (!this.danmakuWs || this.danmakuWs.readyState !== WebSocket.OPEN) {
      console.error('弹幕连接未建立或未处于开启状态');
      return false;
    }

    const danmaku = {
      type: 'DANMAKU',
      data: {
        content,
        color,
        fontSize,
        mode
      }
    };

    this.danmakuWs.send(JSON.stringify(danmaku));
    return true;
  }

  // 尝试重连
  attemptReconnect(type) {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.log(`超过最大重连次数(${this.maxReconnectAttempts})，停止重连`);
      return;
    }

    this.reconnectAttempts++;

    const delay = this.reconnectDelay * Math.pow(1.5,
        this.reconnectAttempts - 1);
    console.log(
        `${type}连接将在${delay}ms后尝试重连，重连次数: ${this.reconnectAttempts}`);

    setTimeout(() => {
      console.log(`正在重连${type}...`);
      this.connect();
    }, delay);
  }

  // 清理资源
  cleanup() {
    // 清理心跳连接
    if (this.heartbeatWs) {
      this.heartbeatWs.close();
      this.heartbeatWs = null;
    }

    // 清理弹幕连接
    if (this.danmakuWs) {
      this.danmakuWs.close();
      this.danmakuWs = null;
    }

    // 清理CDN信息连接
    if (this.cdnWs) {
      this.cdnWs.close();
      this.cdnWs = null;
    }

    // 清理心跳定时器
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
  }

  // 注册弹幕回调
  onDanmaku(callback) {
    this.onDanmakuCallbacks.push(callback);
  }

  // 注册用户加入回调
  onUserJoin(callback) {
    this.onUserJoinCallbacks.push(callback);
  }

  // 注册用户离开回调
  onUserLeave(callback) {
    this.onUserLeaveCallbacks.push(callback);
  }

  // 注册CDN信息回调
  onCdnInfo(callback) {
    this.onCdnInfoCallbacks.push(callback);
  }

  // 关闭连接
  close() {
    this.cleanup();
  }
}

// 使用示例
async function initBarrage(roomId, userId) {
  const barrageClient = new BarrageClient(roomId, userId);

  // 注册弹幕回调
  barrageClient.onDanmaku((data) => {
    console.log('收到弹幕:', data);
    // 在界面上显示弹幕
    displayDanmaku(data);
  });

  // 注册用户加入回调
  barrageClient.onUserJoin((data) => {
    console.log('用户加入:', data);
    // 显示用户加入通知
    showUserJoinNotification(data);
  });

  // 注册用户离开回调
  barrageClient.onUserLeave((data) => {
    console.log('用户离开:', data);
    // 显示用户离开通知
    showUserLeaveNotification(data);
  });

  // 注册CDN信息回调
  barrageClient.onCdnInfo((data) => {
    console.log('CDN信息更新:', data);
    // 更新播放器CDN
    updatePlayerCdn(data);
  });

  // 连接弹幕系统
  const connected = await barrageClient.connect();

  if (connected) {
    console.log('连接弹幕系统成功');

    // 返回客户端实例
    return barrageClient;
  } else {
    console.error('连接弹幕系统失败');
    return null;
  }
}

// 显示弹幕
function displayDanmaku(danmaku) {
  // 创建弹幕元素
  const danmakuElem = document.createElement('div');
  danmakuElem.className = 'danmaku';
  danmakuElem.textContent = danmaku.data.content;

  // 设置样式
  danmakuElem.style.color = danmaku.data.color || '#FFFFFF';
  danmakuElem.style.fontSize = `${danmaku.data.fontSize || 24}px`;

  // 设置弹幕模式
  switch (danmaku.data.mode) {
    case 1: // 滚动弹幕
      danmakuElem.className += ' danmaku-scrolling';
      danmakuElem.style.top = `${Math.floor(Math.random() * 80)}%`;
      break;
    case 2: // 顶部固定弹幕
      danmakuElem.className += ' danmaku-top';
      break;
    case 3: // 底部固定弹幕
      danmakuElem.className += ' danmaku-bottom';
      break;
  }

  // 添加到容器
  const container = document.getElementById('danmakuContainer');
  container.appendChild(danmakuElem);

  // 根据模式自动移除
  if (danmaku.data.mode === 1) {
    // 滚动弹幕移动完成后自动移除
    setTimeout(() => {
      if (danmakuElem.parentNode) {
        danmakuElem.parentNode.removeChild(danmakuElem);
      }
    }, 10000);
  } else {
    // 固定弹幕显示一段时间后移除
    setTimeout(() => {
      if (danmakuElem.parentNode) {
        danmakuElem.parentNode.removeChild(danmakuElem);
      }
    }, 5000);
  }
}

// 显示用户加入通知
function showUserJoinNotification(data) {
  console.log(`用户 ${data.userId} 加入了房间`);
  // 实现加入通知UI
}

// 显示用户离开通知
function showUserLeaveNotification(data) {
  console.log(`用户 ${data.userId} 离开了房间`);
  // 实现离开通知UI
}

// 更新播放器CDN
function updatePlayerCdn(data) {
  // 获取当前CDN线路
  const currentLine = data.data.current;

  // 获取对应的URL
  const cdnLine = data.data.lines.find(line => line.name === currentLine);

  if (cdnLine) {
    // 更新播放器源
    const player = document.getElementById('videoPlayer');
    player.src = cdnLine.url;
    player.load();
    player.play();

    console.log(`已切换到CDN线路: ${currentLine}, URL: ${cdnLine.url}`);
  }
}