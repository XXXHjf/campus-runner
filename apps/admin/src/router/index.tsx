/**
 * 路由配置
 */

/* eslint-disable react-refresh/only-export-components */

import { createBrowserRouter, Navigate } from 'react-router-dom'
import Login from '../pages/Login/Login'
import MainLayout from '../layouts/MainLayout/MainLayout'
import DashboardNew from '../pages/Dashboard/DashboardNew'
import ComingSoon from '../pages/ComingSoon/ComingSoon'
import CategoryManagement from '../pages/CategoryManagement/CategoryManagement'
import UserManagement from '../pages/UserManagement/UserManagement'
import OrderManagement from '../pages/OrderManagement/OrderManagement'
import TakeOrderManagement from '../pages/TakeOrderManagement/TakeOrderManagement'
import SchoolManagement from '../pages/SchoolManagement/SchoolManagement'
import AuthManagement from '../pages/AuthManagement/AuthManagement'
import BannerManagement from '../pages/BannerManagement/BannerManagement'
import SystemConfigManagement from '../pages/SystemConfigManagement/SystemConfigManagement'
import { tokenManager } from '../utils/token'

// 路由守卫：需要登录才能访问
function PrivateRoute({ children }: { children: React.ReactNode }) {
  const isAuthenticated = tokenManager.hasToken()
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />
}

// 路由守卫：已登录则跳转到首页
function PublicRoute({ children }: { children: React.ReactNode }) {
  const isAuthenticated = tokenManager.hasToken()
  return isAuthenticated ? <Navigate to="/dashboard" replace /> : <>{children}</>
}

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Navigate to="/dashboard" replace />,
  },
  {
    path: '/login',
    element: (
      <PublicRoute>
        <Login />
      </PublicRoute>
    ),
  },
  {
    path: '/',
    element: (
      <PrivateRoute>
        <MainLayout />
      </PrivateRoute>
    ),
    children: [
      // 数据概览
      {
        path: 'dashboard',
        element: <DashboardNew />,
      },

      // 订单管理
      {
        path: 'orders/all',
        element: <OrderManagement />,
      },
      {
        path: 'orders/pending',
        element: <OrderManagement />,
      },
      {
        path: 'orders/progress',
        element: <OrderManagement />,
      },
      {
        path: 'orders/completed',
        element: <OrderManagement />,
      },
      {
        path: 'orders/stats',
        element: <OrderManagement />,
      },
      {
        path: 'orders/canceled',
        element: <OrderManagement />,
      },

      // 接单管理
      {
        path: 'takes/all',
        element: <TakeOrderManagement />,
      },
      {
        path: 'takes/withdrawn',
        element: <TakeOrderManagement />,
      },
      {
        path: 'takes/stats',
        element: <TakeOrderManagement />,
      },

      // 用户管理
      {
        path: 'users/all',
        element: <UserManagement />,
      },
      {
        path: 'users/authenticated',
        element: <UserManagement />,
      },
      {
        path: 'users/pending-auth',
        element: <UserManagement />,
      },
      {
        path: 'users/stats',
        element: <UserManagement />,
      },

      // 审核管理
      {
        path: 'auth',
        element: <AuthManagement />,
      },

      // 地址管理
      {
        path: 'address/school',
        element: <SchoolManagement />,
      },

      // 分类管理
      {
        path: 'category',
        element: <CategoryManagement />,
      },

      // 财务管理
      {
        path: 'finance/overview',
        element: <ComingSoon />,
      },
      {
        path: 'finance/income',
        element: <ComingSoon />,
      },
      {
        path: 'finance/withdraw',
        element: <ComingSoon />,
      },

      // 消息管理
      {
        path: 'message/subscribe',
        element: <ComingSoon />,
      },
      {
        path: 'message/template',
        element: <ComingSoon />,
      },

      // 系统设置
      {
        path: 'system/config',
        element: <SystemConfigManagement />,
      },
      {
        path: 'banners',
        element: <BannerManagement />,
      },
      {
        path: 'system/admin',
        element: <ComingSoon />,
      },
      {
        path: 'system/logs',
        element: <ComingSoon />,
      },
    ],
  },
  {
    path: '*',
    element: <Navigate to="/dashboard" replace />,
  },
])
