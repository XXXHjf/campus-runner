import type { ReactNode } from 'react'
import { Empty } from 'antd'

export function createAdminTableLocale(description: ReactNode = '暂无数据') {
  return {
    emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={description} />,
  }
}

