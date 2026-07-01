import { useState } from 'react'
import reactLogo from './assets/react.svg'
import viteLogo from '/vite.svg'
import './App.css'

function App() {
  const [count, setCount] = useState(0)

  return (
    <>
      <div>
        <a href="https://vite.dev" target="_blank">
          <img src={viteLogo} className="logo" alt="Vite logo" />
        </a>
        <a href="https://react.dev" target="_blank">
          <img src={reactLogo} className="logo react" alt="React logo" />
        </a>
      </div>
      <h1>帮帮校园送 - 中后台管理系统</h1>

      <div className="card">
        <button onClick={() => setCount((count) => count + 1)}>计数：{count}</button>
        <p>
          编辑 <code>src/App.tsx</code> 并保存以测试热更新
        </p>
      </div>

      <div className="card">
        <h2>📦 已集成功能</h2>
        <ul style={{ textAlign: 'left', lineHeight: '2' }}>
          <li>✅ TypeScript 类型定义（用户、订单、地址、接单、分类）</li>
          <li>✅ Axios 请求封装（自动 Token、错误处理、拦截器）</li>
          <li>✅ API 服务层（用户、订单、地址、接单、分类、学校）</li>
          <li>✅ 工具函数（存储、Token、验证、格式化）</li>
          <li>✅ 自定义 Hooks（useRequest、usePagination、useAuth）</li>
          <li>✅ 认证上下文（AuthContext）</li>
          <li>✅ 常量定义（状态、消息、配置）</li>
          <li>✅ 环境变量配置</li>
        </ul>
      </div>

      <div className="card">
        <h2>📚 使用指南</h2>
        <p style={{ textAlign: 'left' }}>
          1. 查看 <code>prompt_docs/PROJECT_GUIDE.md</code> 作为唯一入口
          <br />
          3. 运行 <code>npm install</code> 安装依赖
          <br />
          4. 配置环境变量（已创建 .env 文件）
          <br />
          5. 启动后端服务
          <br />
          6. 开始开发你的管理系统！
        </p>
      </div>

      <p className="read-the-docs">查看文档开始使用完整的 API 封装</p>
    </>
  )
}

export default App
