import type { ThemeConfig } from 'antd'

export const adminTheme: ThemeConfig = {
  token: {
    colorPrimary: '#1677ff',
    colorInfo: '#1677ff',
    colorSuccess: '#10b981',
    colorWarning: '#f59e0b',
    colorError: '#ef4444',
    colorText: '#1f2937',
    colorTextSecondary: '#6b7280',
    colorBorder: '#e5e7eb',
    colorBgLayout: '#f5f7fa',
    borderRadius: 8,
    controlHeight: 40,
    fontSize: 14,
    fontFamily:
      "-apple-system, BlinkMacSystemFont, 'Segoe UI', 'Microsoft YaHei', Roboto, sans-serif",
  },
  components: {
    Button: {
      primaryShadow: '0 4px 12px rgba(22, 119, 255, 0.18)',
    },
    Card: {
      borderRadiusLG: 8,
    },
    Table: {
      headerBg: '#fafafa',
      headerColor: '#1f2937',
      rowHoverBg: '#fafcff',
      cellPaddingBlock: 14,
      cellPaddingInline: 18,
    },
    Tabs: {
      horizontalItemPadding: '12px 0',
      horizontalItemGutter: 32,
    },
  },
}

