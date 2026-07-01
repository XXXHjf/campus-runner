/**
 * 学校管理页面
 * 展开/收起按学校分组的地址数据
 */

import { useEffect, useMemo, useState } from 'react'
import {
  getSchools,
  getBuildingsBySchool,
  updateBuilding,
  deleteBuilding,
  createPresetAddress,
} from '../../services/address.service'
import type { AdminSchool, AdminAddressBuilding } from '../../types'
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

export default function SchoolManagement() {
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
    const allRows: {
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
    }[] = []

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

  const renderActions = (row: {
    id: string
    expanded?: boolean
    loading?: boolean
    buildingId?: number
    buildingName?: string
    schoolId?: number
  }) => {
    if (!row.id.startsWith('school-')) {
      return (
        <div className="table-actions">
          <button
            className="btn-action btn-edit"
            onClick={(e) => {
              e.stopPropagation()
              void handleEditBuilding(row)
            }}
            disabled={actionLoadingId === row.id}
          >
            ✏️ 编辑
          </button>
          <button
            className="btn-action btn-delete"
            onClick={(e) => {
              e.stopPropagation()
              void handleDeleteBuilding(row)
            }}
            disabled={actionLoadingId === row.id}
          >
            🗑️ 删除
          </button>
        </div>
      )
    }

    const schoolId = Number(row.id.replace('school-', ''))
    return (
      <div className="table-actions">
        <button
          className="btn-action btn-edit"
          onClick={() => toggleExpand(schoolId)}
          disabled={row.loading}
        >
          {row.expanded ? '收起' : '展开'}
        </button>
      </div>
    )
  }

  return (
    <div className="school-management">
      <div className="page-header">
        <div className="header-left">
          <h1>学校管理</h1>
          <p>展开查看各学校下的地址</p>
        </div>
      </div>

      <div className="toolbar">
        <div className="toolbar-left">
          <div className="search-box">
            <span className="search-icon">🔍</span>
            <input
              type="text"
              placeholder="按名称搜索学校/校区/类型/楼宇..."
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
            />
          </div>
        </div>
        <div className="toolbar-right">
          <span className="data-count">
            {loadingSchools ? '加载中...' : `共 ${filteredSchools.length} 所学校`}
          </span>
          <button
            className="btn-primary"
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
          </button>
        </div>
      </div>

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>学校</th>
              <th>校区</th>
              <th>类型</th>
              <th>楼宇</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 ? (
              <tr>
                <td colSpan={6} className="no-data">
                  <div className="no-data-content">
                    <span className="no-data-icon">📭</span>
                    <p>暂无数据</p>
                    <small>请检查接口或搜索条件</small>
                  </div>
                </td>
              </tr>
            ) : (
              rows.map((row) => (
                <tr
                  key={row.id}
                  className={row.isGroup ? 'group-row' : ''}
                  onClick={() =>
                    row.isGroup ? toggleExpand(Number(row.id.replace('school-', ''))) : undefined
                  }
                >
                  <td>
                    {row.isGroup ? (
                      <span className="table-school">
                        {row.expanded ? '▼ ' : '▶ '} {row.schoolName}
                      </span>
                    ) : (
                      row.schoolName || '-'
                    )}
                  </td>
                  <td>{row.compusName || (row.isGroup ? '-' : '')}</td>
          <td>{row.buildCategoryName || (row.isGroup ? '-' : '')}</td>
          <td>{row.buildingName || (row.isGroup ? '-' : '')}</td>
          <td className="table-time">{row.createdAt || '-'}</td>
          <td className="table-actions-cell">{renderActions(row)}</td>
        </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="notice-box">
        <span className="notice-icon">💡</span>
        <div className="notice-content">
          <strong>功能说明：</strong>
          <ul>
            <li>点击学校行展开/收起该学校下的楼宇列表</li>
            <li>使用接口：`getSchools`、`getBuildingsBySchool`</li>
            <li>编辑、删除楼宇已支持（调用 `updateBuilding` / `deleteBuilding`）</li>
          </ul>
        </div>
      </div>

      {editModal.open && (
        <div className="modal-overlay" onClick={() => setEditModal({ open: false })}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>编辑楼宇</h2>
              <button className="modal-close" onClick={() => setEditModal({ open: false })}>
                ✕
              </button>
            </div>
            <div className="modal-body">
              <div className="form-group">
                <label>楼宇名称</label>
                <input
                  type="text"
                  value={editName}
                  onChange={(e) => setEditName(e.target.value)}
                  placeholder="请输入新的楼宇名称"
                />
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setEditModal({ open: false })}>
                取消
              </button>
              <button
                className="btn-primary"
                onClick={async () => {
                  if (!editModal.buildingId || !editModal.schoolId) return
                  if (!editName.trim()) return
                  setActionLoadingId(`building-${editModal.buildingId}`)
                  try {
                    await updateBuilding({ buildingID: editModal.buildingId, buildingName: editName.trim() })
                    await refreshSchoolBuildings(editModal.schoolId)
                    setEditModal({ open: false })
                  } catch (error) {
                    const message = isRepeatError(error)
                      ? '楼宇名称已存在，请检查后再试'
                      : '更新失败，请稍后再试'
                    console.error('更新楼宇失败', error)
                    alert(message)
                  } finally {
                    setActionLoadingId(null)
                  }
                }}
                disabled={actionLoadingId === `building-${editModal.buildingId}`}
              >
                保存
              </button>
            </div>
          </div>
        </div>
      )}

      {deleteModal.open && (
        <div className="modal-overlay" onClick={() => setDeleteModal({ open: false })}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>删除确认</h2>
              <button className="modal-close" onClick={() => setDeleteModal({ open: false })}>
                ✕
              </button>
            </div>
            <div className="modal-body">
              <p>确认删除该楼宇吗？删除后不可恢复。</p>
            </div>
            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setDeleteModal({ open: false })}>
                取消
              </button>
              <button
                className="btn-primary"
                onClick={async () => {
                  if (!deleteModal.buildingId || !deleteModal.schoolId) return
                  setActionLoadingId(`building-${deleteModal.buildingId}`)
                  try {
                    await deleteBuilding(deleteModal.buildingId)
                    await refreshSchoolBuildings(deleteModal.schoolId)
                    setDeleteModal({ open: false })
                  } catch (error) {
                    console.error('删除楼宇失败', error)
                    alert('删除失败，请稍后再试')
                  } finally {
                    setActionLoadingId(null)
                  }
                }}
                disabled={actionLoadingId === `building-${deleteModal.buildingId}`}
              >
                确认删除
              </button>
            </div>
          </div>
        </div>
      )}

      {addModal.open && (
        <div className="modal-overlay" onClick={() => setAddModal((prev) => ({ ...prev, open: false }))}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>添加地址</h2>
              <button
                className="modal-close"
                onClick={() => setAddModal((prev) => ({ ...prev, open: false }))}
              >
                ✕
              </button>
            </div>
            <div className="modal-body">
              {addMessage && (
                <div className={`alert ${addMessage.type === 'success' ? 'alert-success' : 'alert-error'}`}>
                  {addMessage.text}
                </div>
              )}

              <div className="form-group">
                <label>选择学校</label>
                <div className="select-or-input">
                  <select
                    value={addModal.selectedSchoolId ?? ''}
                    onChange={(e) =>
                      setAddModal((prev) => ({
                        ...prev,
                        selectedSchoolId: e.target.value ? Number(e.target.value) : undefined,
                        schoolNameInput: e.target.value
                          ? schools.find((item) => item.school.id === Number(e.target.value))?.school.schoolName ||
                            ''
                          : prev.schoolNameInput,
                      }))
                    }
                  >
                    <option value="">从下拉选择</option>
                    {schools.map((item) => (
                      <option key={item.school.id} value={item.school.id}>
                        {item.school.schoolName}
                      </option>
                    ))}
                  </select>
                  <span className="inline-sep">或</span>
                  <input
                    type="text"
                    value={addModal.schoolNameInput}
                    onChange={(e) =>
                      setAddModal((prev) => ({
                        ...prev,
                        selectedSchoolId: undefined,
                        schoolNameInput: e.target.value,
                      }))
                    }
                    placeholder="手动输入学校名称"
                  />
                </div>
              </div>
              <div className="form-group">
                <label>校区</label>
                <input
                  type="text"
                  value={addModal.compusName}
                  onChange={(e) =>
                    setAddModal((prev) => ({ ...prev, compusName: e.target.value }))
                  }
                  placeholder="请输入校区"
                />
              </div>
              <div className="form-group">
                <label>类型</label>
                <input
                  type="text"
                  value={addModal.buildCategoryName}
                  onChange={(e) =>
                    setAddModal((prev) => ({ ...prev, buildCategoryName: e.target.value }))
                  }
                  placeholder="例如：宿舍楼、教学楼"
                />
              </div>
              <div className="form-group">
                <label>楼宇</label>
                <input
                  type="text"
                  value={addModal.buildingName}
                  onChange={(e) =>
                    setAddModal((prev) => ({ ...prev, buildingName: e.target.value }))
                  }
                  placeholder="请输入楼宇名称"
                />
              </div>
            </div>
            <div className="modal-footer">
              <button
                className="btn-secondary"
                onClick={() => setAddModal((prev) => ({ ...prev, open: false }))}
                disabled={addSubmitting}
              >
                取消
              </button>
              <button
                className="btn-primary"
                disabled={addSubmitting}
                onClick={async () => {
                  const schoolNameFromSelect = schools.find(
                    (item) => item.school.id === addModal.selectedSchoolId,
                  )?.school.schoolName
                  const finalSchoolName = schoolNameFromSelect || addModal.schoolNameInput.trim()
                  if (!finalSchoolName || !addModal.compusName.trim() || !addModal.buildCategoryName.trim() || !addModal.buildingName.trim()) {
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
                      selectedSchoolId: prev.selectedSchoolId,
                      schoolNameInput: prev.selectedSchoolId ? finalSchoolName : prev.schoolNameInput,
                      compusName: prev.compusName,
                      buildCategoryName: prev.buildCategoryName,
                      buildingName: '',
                    }))
                  } catch (error) {
                    const text = isRepeatError(error)
                      ? '地址重复，请检查后再试'
                      : '添加失败，请稍后再试'
                    console.error('添加地址失败', error)
                    setAddMessage({ type: 'error', text })
                  } finally {
                    setAddSubmitting(false)
                  }
                }}
              >
                保存
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
