/**
 * 系统配置管理
 * 当前支持：服务费率、最低服务费
 * 采用配置项驱动渲染，便于后续扩展更多系统配置项
 */

import { useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Col,
  Form,
  Input,
  InputNumber,
  Modal,
  Row,
  Space,
  Spin,
  Statistic,
  Tag,
} from 'antd'
import { systemConfigService } from '../../services'
import { userService } from '../../services'
import { useAuthContext } from '../../contexts/AuthContext'
import type { SystemConfigFieldDefinition, SystemConfigKey } from '../../types'
import { AdminPage, AdminPageHeader } from '../../components/admin'
import './SystemConfigManagement.css'

const configFields: SystemConfigFieldDefinition[] = [
  {
    key: 'serviceFeeRate',
    label: '服务费率',
    description: '用于计算订单服务费，例如 0.05 代表 5% 的服务费率。',
    placeholder: '请输入服务费率，例如 0.05',
    min: 0,
    step: '0.01',
  },
  {
    key: 'serviceFeeMin',
    label: '最低服务费',
    description: '当按费率计算结果低于该值时，按此最低服务费收取。',
    placeholder: '请输入最低服务费，例如 0.5',
    unit: '元',
    min: 0,
    step: '0.01',
  },
]

function getErrorMessage(err: unknown, fallback: string) {
  if (err instanceof Error) return err.message
  if (err && typeof err === 'object' && 'message' in err) {
    const record = err as Record<string, unknown>
    if (typeof record.message === 'string') return record.message
  }
  return fallback
}

function parseNumberValue(value: string): number | null {
  const trimmed = value.trim()
  if (!trimmed) return null
  const parsed = Number(trimmed)
  if (!Number.isFinite(parsed)) return null
  return parsed
}

function getTokenFromLoginResponse(data: unknown): string | null {
  if (!data || typeof data !== 'object') return null
  const record = data as Record<string, unknown>
  const adminToken = record.adminToken
  if (typeof adminToken === 'string' && adminToken) return adminToken
  const token = record.token
  if (typeof token === 'string' && token) return token
  if (record.data && typeof record.data === 'object') {
    const nested = record.data as Record<string, unknown>
    const nestedAdminToken = nested.adminToken
    if (typeof nestedAdminToken === 'string' && nestedAdminToken) return nestedAdminToken
    const nestedToken = nested.token
    if (typeof nestedToken === 'string' && nestedToken) return nestedToken
  }
  return null
}

export default function SystemConfigManagement() {
  const { userInfo } = useAuthContext()
  const [loading, setLoading] = useState(false)
  const [savingKey, setSavingKey] = useState<SystemConfigKey | null>(null)
  const [globalError, setGlobalError] = useState<string | null>(null)
  const [confirming, setConfirming] = useState(false)

  const [currentValues, setCurrentValues] = useState<Record<SystemConfigKey, string>>({
    serviceFeeRate: '',
    serviceFeeMin: '',
  })
  const [editValues, setEditValues] = useState<Record<SystemConfigKey, string>>({
    serviceFeeRate: '',
    serviceFeeMin: '',
  })
  const [fieldErrors, setFieldErrors] = useState<Partial<Record<SystemConfigKey, string>>>({})
  const [successKey, setSuccessKey] = useState<SystemConfigKey | null>(null)
  const [confirmModal, setConfirmModal] = useState<{
    open: boolean
    field?: SystemConfigFieldDefinition
    value?: number
    password: string
    error: string
  }>({
    open: false,
    password: '',
    error: '',
  })

  const loadConfigValues = async () => {
    setLoading(true)
    setGlobalError(null)
    setSuccessKey(null)

    try {
      const values = await Promise.all(
        configFields.map(async (field) => {
          const value = await systemConfigService.getConfigValue(field.key)
          return [field.key, value] as const
        }),
      )

      const nextValues: Record<SystemConfigKey, string> = {
        serviceFeeRate: '',
        serviceFeeMin: '',
      }

      values.forEach(([key, value]) => {
        nextValues[key] = value
      })

      setCurrentValues(nextValues)
      setEditValues(nextValues)
      setFieldErrors({})
    } catch (err: unknown) {
      console.error('加载系统配置失败', err)
      setGlobalError(getErrorMessage(err, '加载系统配置失败，请稍后重试'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadConfigValues()
  }, [])

  const changedKeys = useMemo(() => {
    return configFields
      .filter((field) => editValues[field.key].trim() !== currentValues[field.key].trim())
      .map((field) => field.key)
  }, [editValues, currentValues])

  const validateField = (field: SystemConfigFieldDefinition, value: string): string | null => {
    const parsed = parseNumberValue(value)
    if (parsed === null) return '请输入有效数字'
    if (typeof field.min === 'number' && parsed < field.min) {
      return `数值不能小于 ${field.min}`
    }
    return null
  }

  const handleInputChange = (key: SystemConfigKey, value: string) => {
    setEditValues((prev) => ({ ...prev, [key]: value }))
    setFieldErrors((prev) => ({ ...prev, [key]: undefined }))
    if (successKey === key) setSuccessKey(null)
  }

  const handleSaveOne = async (field: SystemConfigFieldDefinition) => {
    const value = editValues[field.key]
    const validationError = validateField(field, value)
    if (validationError) {
      setFieldErrors((prev) => ({ ...prev, [field.key]: validationError }))
      return
    }

    const parsed = parseNumberValue(value)
    if (parsed === null) return

    setConfirmModal({
      open: true,
      field,
      value: parsed,
      password: '',
      error: '',
    })
  }

  const closeConfirmModal = () => {
    if (confirming) return
    setConfirmModal({
      open: false,
      password: '',
      error: '',
    })
  }

  const handleConfirmSave = async () => {
    if (!confirmModal.open || !confirmModal.field || typeof confirmModal.value !== 'number') return
    const password = confirmModal.password
    if (!password) {
      setConfirmModal((prev) => ({ ...prev, error: '请输入密码' }))
      return
    }

    setConfirming(true)
    setGlobalError(null)
    setFieldErrors((prev) => ({ ...prev, [confirmModal.field!.key]: undefined }))

    try {
      const candidateUsernames = [userInfo?.username?.trim(), 'admin'].filter(
        (item, index, arr): item is string => !!item && arr.indexOf(item) === index,
      )
      let verified = false
      for (const username of candidateUsernames) {
        try {
          const loginResult = await userService.login({ username, password })
          const token = getTokenFromLoginResponse(loginResult)
          if (token) {
            verified = true
            break
          }
        } catch {
          // ignore and continue
        }
      }
      if (!verified) {
        throw new Error('密码验证失败，请重试')
      }
    } catch (err: unknown) {
      setConfirmModal((prev) => ({
        ...prev,
        error: getErrorMessage(err, '密码验证失败，请重试'),
      }))
      setConfirming(false)
      return
    }

    const targetField = confirmModal.field
    const targetValue = confirmModal.value

    setSavingKey(targetField.key)
    setConfirming(false)
    setConfirmModal({
      open: false,
      password: '',
      error: '',
    })

    try {
      await systemConfigService.updateConfigValue(targetField.key, targetValue)
      const normalized = String(targetValue)
      setCurrentValues((prev) => ({ ...prev, [targetField.key]: normalized }))
      setEditValues((prev) => ({ ...prev, [targetField.key]: normalized }))
      setSuccessKey(targetField.key)
    } catch (err: unknown) {
      console.error(`保存配置失败: ${targetField.key}`, err)
      setGlobalError(getErrorMessage(err, `保存${targetField.label}失败，请稍后重试`))
    } finally {
      setSavingKey(null)
    }
  }

  const handleResetOne = (field: SystemConfigFieldDefinition) => {
    setEditValues((prev) => ({ ...prev, [field.key]: currentValues[field.key] }))
    setFieldErrors((prev) => ({ ...prev, [field.key]: undefined }))
    if (successKey === field.key) setSuccessKey(null)
  }

  return (
    <AdminPage className="system-config-management">
      <AdminPageHeader
        title="系统配置"
        description="管理系统级业务参数，修改后将影响相关业务流程"
        actions={
          <Button onClick={loadConfigValues} loading={loading}>
            刷新
          </Button>
        }
      />

      <Row gutter={16}>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic title="配置项数量" value={configFields.length} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="未保存变更"
              value={changedKeys.length}
              styles={{
                content: changedKeys.length > 0 ? { color: '#d97706' } : undefined,
              }}
            />
          </Card>
        </Col>
      </Row>

      {globalError && (
        <Alert type="error" showIcon title={globalError} />
      )}

      <div className="config-list">
        {configFields.map((field) => {
          const isSaving = savingKey === field.key
          const changed = editValues[field.key].trim() !== currentValues[field.key].trim()
          const fieldError = fieldErrors[field.key]
          const showSuccess = successKey === field.key && !changed

          return (
            <Card
              key={field.key}
              title={
                <div className="config-card-title">
                  <span>{field.label}</span>
                  <small>{field.description}</small>
                </div>
              }
              extra={
                <Tag color={changed ? 'gold' : 'green'}>
                  {changed ? '待保存' : '已同步'}
                </Tag>
              }
            >
              <div className="config-row">
                <div className="config-current">
                  <span className="field-label">当前值</span>
                  <span className="field-value">
                    {currentValues[field.key] || '-'}
                    {field.unit ? ` ${field.unit}` : ''}
                  </span>
                </div>

                <div className="config-editor">
                  <label htmlFor={field.key} className="field-label">
                    新值
                  </label>
                  <Space.Compact block>
                    <InputNumber
                      id={field.key}
                      stringMode
                      step={field.step ?? '0.01'}
                      min={typeof field.min === 'number' ? String(field.min) : undefined}
                      placeholder={field.placeholder}
                      value={editValues[field.key] || null}
                      onChange={(value) => handleInputChange(field.key, String(value ?? ''))}
                      disabled={isSaving || loading}
                      status={fieldError ? 'error' : undefined}
                      style={{ width: '100%' }}
                    />
                    {field.unit && <Button disabled>{field.unit}</Button>}
                  </Space.Compact>
                  {fieldError && <p className="field-error">{fieldError}</p>}
                  {showSuccess && <p className="field-success">保存成功</p>}
                </div>

                <div className="config-actions">
                  <Button
                    onClick={() => handleResetOne(field)}
                    disabled={isSaving || loading || !changed}
                  >
                    还原
                  </Button>
                  <Button
                    type="primary"
                    onClick={() => handleSaveOne(field)}
                    disabled={isSaving || loading || !changed}
                    loading={isSaving}
                  >
                    保存
                  </Button>
                </div>
              </div>
            </Card>
          )
        })}
      </div>

      {loading && <Spin className="config-loading" description="加载中" />}

      <Modal
        title="敏感操作确认"
        open={confirmModal.open && Boolean(confirmModal.field)}
        onCancel={closeConfirmModal}
        onOk={handleConfirmSave}
        okText="验证并保存"
        cancelText="取消"
        confirmLoading={confirming}
        mask={{ closable: !confirming }}
      >
        {confirmModal.open && confirmModal.field && (
          <>
            <p className="system-config-confirm-text">
              即将修改“{confirmModal.field.label}”为 <b>{confirmModal.value}</b>
              {confirmModal.field.unit ? ` ${confirmModal.field.unit}` : ''}
              ，请输入账号密码完成二次验证。
            </p>
            <Form layout="vertical">
              <Form.Item
                label="密码"
                validateStatus={confirmModal.error ? 'error' : undefined}
                help={confirmModal.error || undefined}
              >
                <Input.Password
                  autoFocus
                  value={confirmModal.password}
                  onChange={(event) =>
                    setConfirmModal((prev) => ({
                      ...prev,
                      password: event.target.value,
                      error: '',
                    }))
                  }
                  disabled={confirming}
                  autoComplete="current-password"
                  placeholder="请输入当前密码"
                  onPressEnter={() => void handleConfirmSave()}
                />
              </Form.Item>
            </Form>
          </>
        )}
      </Modal>
    </AdminPage>
  )
}
