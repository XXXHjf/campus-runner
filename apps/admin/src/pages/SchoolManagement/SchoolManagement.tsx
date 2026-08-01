/**
 * 学校管理页面
 * 展开/收起按学校分组的地址数据
 */

import { useEffect, useMemo, useState } from 'react'
import { Alert, App, Button, Form, Input, Modal, Select, Space, Table } from 'antd'
import {
  DeleteOutlined,
  DownOutlined,
  EditOutlined,
  PlusOutlined,
  RightOutlined,
} from '@ant-design/icons'
import {
  getSchools,
  getBuildingsBySchool,
  updateBuilding,
  deleteBuilding,
  createPresetAddress,
} from '../../services/address.service'
import type { AdminSchool, AdminAddressBuilding } from '../../types'
import {
  AdminContentCard,
  AdminCount,
  AdminFilterBar,
  AdminPage,
  AdminPageHeader,
  createAdminTableLocale,
} from '../../components/admin'
import './SchoolManagement.css'

function isRepeatError(error: unknown) {
  if (!error || typeof error !== 'object') return false
  const record = error as Record<string, unknown>
  return record.msg === 'REPEAT' || record.message === 'REPEAT' || record.code === 0
}

interface SchoolWithChildren {
  school: AdminSchool
  buildings: AdminAddressBuilding[]
  loading: boolean
  expanded: boolean
}

interface SchoolTableRow {
  id: string
  schoolName: string
  compusName?: string
  buildCategoryName?: string
  buildingName?: string
  createdAt?: string
  isGroup?: boolean
  expanded?: boolean
  loading?: boolean
  schoolId?: number
  buildingId?: number
}

export default function SchoolManagement() {
  const { message } = App.useApp()
  const [schools, setSchools] = useState<SchoolWithChildren[]>([])
  const [loadingSchools, setLoadingSchools] = useState(false)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [actionLoadingId, setActionLoadingId] = useState<string | null>(null)
  const [editModal, setEditModal] = useState<{
    open: boolean
    buildingId?: number
    schoolId?: number
    name?: string
  }>({ open: false })
  const [deleteModal, setDeleteModal] = useState<{ open: boolean; buildingId?: number; schoolId?: number }>(
    { open: false },
  )
  const [editName, setEditName] = useState('')
  const [addModal, setAddModal] = useState<{
    open: boolean
    selectedSchoolId?: number
    schoolNameInput: string
    compusName: string
    buildCategoryName: string
    buildingName: string
  }>({
    open: false,
    schoolNameInput: '',
    compusName: '',
    buildCategoryName: '',
    buildingName: '',
  })
  const [addSubmitting, setAddSubmitting] = useState(false)
  const [addMessage, setAddMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(
    null,
  )

  useEffect(() => {
    loadSchools()
  }, [])

  const loadSchools = async (preserveState = false) => {
    setLoadingSchools(true)
    try {
      const list = await getSchools()
      setSchools((prev) => {
        const prevMap = new Map<number, SchoolWithChildren>()
        prev.forEach((item) => prevMap.set(item.school.id, item))
        return list.map((item) => {
          const existed = prevMap.get(item.id)
          if (preserveState && existed) {
            return {
              ...existed,
              school: item,
            }
          }
          return {
            school: item,
            buildings: [],
            loading: false,
            expanded: false,
          }
        })
      })
    } catch (error) {
      console.error('获取学校列表失败', error)
    } finally {
      setLoadingSchools(false)
    }
  }

  const toggleExpand = async (schoolId: number) => {
    setSchools((prev) =>
      prev.map((item) =>
        item.school.id === schoolId ? { ...item, expanded: !item.expanded } : item,
      ),
    )

    const target = schools.find((s) => s.school.id === schoolId)
    if (!target) return
    if (target.expanded || target.buildings.length > 0) return

    setSchools((prev) =>
      prev.map((item) =>
        item.school.id === schoolId ? { ...item, loading: true } : item,
      ),
    )

    try {
      const data = await getBuildingsBySchool(schoolId)
      setSchools((prev) =>
        prev.map((item) =>
          item.school.id === schoolId
            ? { ...item, buildings: data || [], loading: false, expanded: true }
            : item,
        ),
      )
    } catch (error) {
      console.error('获取楼宇列表失败', error)
      setSchools((prev) =>
        prev.map((item) =>
          item.school.id === schoolId ? { ...item, loading: false } : item,
        ),
      )
    }
  }

  const filteredSchools = useMemo(() => {
    if (!searchKeyword) return schools
    return schools
      .map((item) => {
        const matchSchool = item.school.schoolName.includes(searchKeyword)
        const filteredBuildings = item.buildings.filter(
          (b) =>
            b.schoolName.includes(searchKeyword) ||
            b.compusName.includes(searchKeyword) ||
            b.buildCategoryName.includes(searchKeyword) ||
            b.buildingName.includes(searchKeyword),
        )
        if (matchSchool) {
          return { ...item, buildings: item.buildings }
        }
        if (filteredBuildings.length > 0) {
          return { ...item, expanded: true, buildings: filteredBuildings }
        }
        return null
      })
      .filter(Boolean) as SchoolWithChildren[]
  }, [schools, searchKeyword])

  const rows = useMemo(() => {
    const allRows: SchoolTableRow[] = []

    filteredSchools.forEach((item) => {
      allRows.push({
        id: `school-${item.school.id}`,
        schoolName: item.school.schoolName,
        compusName: '',
        buildCategoryName: '',
        buildingName: '',
        createdAt: '',
        isGroup: true,
        expanded: item.expanded,
        loading: item.loading,
        schoolId: item.school.id,
      })

      if (item.expanded) {
        if (item.loading) {
          allRows.push({
            id: `loading-${item.school.id}`,
            schoolName: '',
            buildCategoryName: '加载中...',
            isGroup: false,
          })
        } else if (item.buildings.length === 0) {
          allRows.push({
            id: `empty-${item.school.id}`,
            schoolName: '',
            buildCategoryName: '暂无楼宇数据',
            isGroup: false,
          })
        } else {
          item.buildings.forEach((b) => {
            allRows.push({
              id: `building-${b.id}`,
              buildingId: b.id,
              schoolId: b.schoolId,
              schoolName: b.schoolName,
              compusName: b.compusName,
              buildCategoryName: b.buildCategoryName,
              buildingName: b.buildingName,
              createdAt: '',
            })
          })
        }
      }
    })

    return allRows
  }, [filteredSchools])

  const refreshSchoolBuildings = async (schoolId: number) => {
    setSchools((prev) =>
      prev.map((item) =>
        item.school.id === schoolId ? { ...item, loading: true, expanded: true } : item,
      ),
    )

    try {
      const data = await getBuildingsBySchool(schoolId)
      setSchools((prev) =>
        prev.map((item) =>
          item.school.id === schoolId
            ? { ...item, buildings: data || [], loading: false, expanded: true }
            : item,
        ),
      )
    } catch (error) {
      console.error('刷新楼宇列表失败', error)
      setSchools((prev) =>
        prev.map((item) =>
          item.school.id === schoolId ? { ...item, loading: false } : item,
        ),
      )
    }
  }

  const handleEditBuilding = async (row: {
    buildingId?: number
    buildingName?: string
    schoolId?: number
  }) => {
    if (!row.buildingId || !row.schoolId) return
    setEditModal({
      open: true,
      buildingId: row.buildingId,
      schoolId: row.schoolId,
      name: row.buildingName || '',
    })
    setEditName(row.buildingName || '')
  }

  const handleDeleteBuilding = async (row: { buildingId?: number; schoolId?: number }) => {
    if (!row.buildingId || !row.schoolId) return
    setDeleteModal({ open: true, buildingId: row.buildingId, schoolId: row.schoolId })
  }

  const renderActions = (row: SchoolTableRow) => {
    if (!row.id.startsWith('school-')) {
      if (!row.buildingId) return null

      return (
        <Space size={4}>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={(event) => {
              event.stopPropagation()
              void handleEditBuilding(row)
            }}
            disabled={actionLoadingId === row.id}
          >
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={(event) => {
              event.stopPropagation()
              void handleDeleteBuilding(row)
            }}
            disabled={actionLoadingId === row.id}
          >
            删除
          </Button>
        </Space>
      )
    }

    const schoolId = Number(row.id.replace('school-', ''))
    return (
      <Button
        type="link"
        size="small"
        onClick={(event) => {
          event.stopPropagation()
          void toggleExpand(schoolId)
        }}
        loading={row.loading}
      >
        {row.expanded ? '收起' : '展开'}
      </Button>
    )
  }

  const columns = [
    {
      title: '学校',
      dataIndex: 'schoolName',
      width: 220,
      render: (value: string, row: SchoolTableRow) =>
        row.isGroup ? (
          <span className="school-group-name">
            {row.expanded ? <DownOutlined /> : <RightOutlined />}
            {value}
          </span>
        ) : (
          value || '-'
        ),
    },
    {
      title: '校区',
      dataIndex: 'compusName',
      width: 180,
      render: (value: string | undefined, row: SchoolTableRow) =>
        row.isGroup ? '-' : value || '-',
    },
    {
      title: '类型',
      dataIndex: 'buildCategoryName',
      width: 180,
      render: (value: string | undefined, row: SchoolTableRow) =>
        row.isGroup ? '-' : value || '-',
    },
    {
      title: '楼宇',
      dataIndex: 'buildingName',
      render: (value: string | undefined, row: SchoolTableRow) =>
        row.isGroup ? '-' : value || '-',
    },
    {
      title: '操作',
      width: 180,
      fixed: 'right' as const,
      render: (_: unknown, row: SchoolTableRow) => renderActions(row),
    },
  ]

  const saveEditedBuilding = async () => {
    if (!editModal.buildingId || !editModal.schoolId || !editName.trim()) return
    setActionLoadingId(`building-${editModal.buildingId}`)
    try {
      await updateBuilding({
        buildingID: editModal.buildingId,
        buildingName: editName.trim(),
      })
      await refreshSchoolBuildings(editModal.schoolId)
      setEditModal({ open: false })
      message.success('楼宇名称已更新')
    } catch (error) {
      const errorMessage = isRepeatError(error)
        ? '楼宇名称已存在，请检查后再试'
        : '更新失败，请稍后再试'
      console.error('更新楼宇失败', error)
      message.error(errorMessage)
    } finally {
      setActionLoadingId(null)
    }
  }

  const confirmDeleteBuilding = async () => {
    if (!deleteModal.buildingId || !deleteModal.schoolId) return
    setActionLoadingId(`building-${deleteModal.buildingId}`)
    try {
      await deleteBuilding(deleteModal.buildingId)
      await refreshSchoolBuildings(deleteModal.schoolId)
      setDeleteModal({ open: false })
      message.success('楼宇已删除')
    } catch (error) {
      console.error('删除楼宇失败', error)
      message.error('删除失败，请稍后再试')
    } finally {
      setActionLoadingId(null)
    }
  }

  const saveAddress = async () => {
    const schoolNameFromSelect = schools.find(
      (item) => item.school.id === addModal.selectedSchoolId,
    )?.school.schoolName
    const finalSchoolName = schoolNameFromSelect || addModal.schoolNameInput.trim()
    if (
      !finalSchoolName ||
      !addModal.compusName.trim() ||
      !addModal.buildCategoryName.trim() ||
      !addModal.buildingName.trim()
    ) {
      setAddMessage({ type: 'error', text: '请填写完整信息' })
      return
    }
    setAddSubmitting(true)
    try {
      await createPresetAddress({
        schoolName: finalSchoolName,
        compusName: addModal.compusName.trim(),
        buildCategoryName: addModal.buildCategoryName.trim(),
        buildingName: addModal.buildingName.trim(),
      })
      await loadSchools(true)
      if (addModal.selectedSchoolId) {
        await refreshSchoolBuildings(addModal.selectedSchoolId)
      }
      setAddMessage({ type: 'success', text: '添加成功，可继续添加' })
      setAddModal((prev) => ({
        ...prev,
        schoolNameInput: prev.selectedSchoolId ? finalSchoolName : prev.schoolNameInput,
        buildingName: '',
      }))
    } catch (error) {
      const text = isRepeatError(error) ? '地址重复，请检查后再试' : '添加失败，请稍后再试'
      console.error('添加地址失败', error)
      setAddMessage({ type: 'error', text })
    } finally {
      setAddSubmitting(false)
    }
  }

  return (
    <AdminPage className="school-management">
      <AdminPageHeader title="学校管理" description="管理学校、校区和楼宇地址" />

      <AdminFilterBar
        extra={
          <>
            <AdminCount>
              {loadingSchools ? '加载中' : `共 ${filteredSchools.length} 所学校`}
            </AdminCount>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() =>
                setAddModal({
                  open: true,
                  selectedSchoolId: undefined,
                  schoolNameInput: '',
                  compusName: '',
                  buildCategoryName: '',
                  buildingName: '',
                })
              }
            >
              添加地址
            </Button>
          </>
        }
      >
        <Input.Search
          allowClear
          placeholder="搜索学校、校区、类型或楼宇"
          value={searchKeyword}
          onChange={(event) => setSearchKeyword(event.target.value)}
          onSearch={setSearchKeyword}
          style={{ width: 380 }}
        />
      </AdminFilterBar>

      <AdminContentCard flush>
        <Table
          rowKey="id"
          loading={loadingSchools}
          dataSource={rows}
          columns={columns}
          locale={createAdminTableLocale('暂无学校地址数据')}
          rowClassName={(row) => (row.isGroup ? 'school-group-row' : '')}
          onRow={(row) => ({
            onClick: () => {
              if (row.isGroup && row.schoolId) void toggleExpand(row.schoolId)
            },
          })}
          pagination={false}
          scroll={{ x: 980 }}
        />
      </AdminContentCard>

      <Alert
        type="info"
        showIcon
        message="点击学校行可展开或收起该学校下的楼宇列表"
      />

      <Modal
        title="编辑楼宇"
        open={editModal.open}
        onCancel={() => setEditModal({ open: false })}
        onOk={saveEditedBuilding}
        okText="保存"
        cancelText="取消"
        confirmLoading={actionLoadingId === `building-${editModal.buildingId}`}
      >
        <Form layout="vertical">
          <Form.Item label="楼宇名称" required>
            <Input
              value={editName}
              onChange={(event) => setEditName(event.target.value)}
              placeholder="请输入新的楼宇名称"
              onPressEnter={() => void saveEditedBuilding()}
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="删除楼宇"
        open={deleteModal.open}
        onCancel={() => setDeleteModal({ open: false })}
        onOk={confirmDeleteBuilding}
        okText="确认删除"
        cancelText="取消"
        okButtonProps={{ danger: true }}
        confirmLoading={actionLoadingId === `building-${deleteModal.buildingId}`}
      >
        <p>确认删除该楼宇吗？删除后不可恢复。</p>
      </Modal>

      <Modal
        title="添加地址"
        open={addModal.open}
        onCancel={() => setAddModal((prev) => ({ ...prev, open: false }))}
        onOk={saveAddress}
        okText="保存"
        cancelText="取消"
        confirmLoading={addSubmitting}
        width={720}
        mask={{ closable: !addSubmitting }}
      >
        {addMessage && (
          <Alert
            className="school-form-alert"
            type={addMessage.type}
            showIcon
            title={addMessage.text}
          />
        )}
        <Form layout="vertical">
          <Form.Item label="学校" required>
            <Space.Compact block>
              <Select
                allowClear
                placeholder="选择已有学校"
                style={{ width: '45%' }}
                value={addModal.selectedSchoolId}
                options={schools.map((item) => ({
                  value: item.school.id,
                  label: item.school.schoolName,
                }))}
                onChange={(value) =>
                  setAddModal((prev) => ({
                    ...prev,
                    selectedSchoolId: value,
                    schoolNameInput: value
                      ? schools.find((item) => item.school.id === value)?.school.schoolName || ''
                      : '',
                  }))
                }
              />
              <Input
                value={addModal.schoolNameInput}
                onChange={(event) =>
                  setAddModal((prev) => ({
                    ...prev,
                    selectedSchoolId: undefined,
                    schoolNameInput: event.target.value,
                  }))
                }
                placeholder="或输入学校名称"
              />
            </Space.Compact>
          </Form.Item>
          <div className="admin-form-grid">
            <Form.Item label="校区" required>
              <Input
                value={addModal.compusName}
                onChange={(event) =>
                  setAddModal((prev) => ({ ...prev, compusName: event.target.value }))
                }
                placeholder="请输入校区"
              />
            </Form.Item>
            <Form.Item label="类型" required>
              <Input
                value={addModal.buildCategoryName}
                onChange={(event) =>
                  setAddModal((prev) => ({
                    ...prev,
                    buildCategoryName: event.target.value,
                  }))
                }
                placeholder="例如：宿舍楼、教学楼"
              />
            </Form.Item>
            <Form.Item className="admin-form-grid__full" label="楼宇" required>
              <Input
                value={addModal.buildingName}
                onChange={(event) =>
                  setAddModal((prev) => ({ ...prev, buildingName: event.target.value }))
                }
                placeholder="请输入楼宇名称"
              />
            </Form.Item>
          </div>
        </Form>
      </Modal>
    </AdminPage>
  )
}
