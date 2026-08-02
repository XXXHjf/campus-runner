import { del, request } from './request'
import { prepareImageForUpload } from '../utils/prepareImageForUpload'

export type MediaPurpose =
  | 'ORDER_CATEGORY_ICON'
  | 'SECOND_HAND_CATEGORY_ICON'
  | 'SECOND_HAND_PRODUCT_IMAGE'
  | 'ORDER_IMAGE'
  | 'DELIVERY_PROOF'
  | 'AVATAR'
  | 'STUDENT_CARD'
  | 'BANNER'

export type MediaUploadResult = {
  mediaId: number
  previewUrl: string
  status: 'TEMP'
  expiresAt: string
}

export async function uploadImage(
  file: File,
  purpose: MediaPurpose,
  onProgress?: (progress: number) => void,
): Promise<MediaUploadResult> {
  const preparedFile = await prepareImageForUpload(file, purpose)
  const formData = new FormData()
  formData.append('file', preparedFile)
  formData.append('purpose', purpose)

  return request<MediaUploadResult>({
    url: '/admin/api/media/images',
    method: 'POST',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    onUploadProgress: (event: { loaded: number; total?: number }) => {
      if (onProgress && event.total) {
        onProgress(Math.round((event.loaded * 100) / event.total))
      }
    },
  })
}

export async function releaseTemporaryImage(mediaId: number): Promise<void> {
  await del(`/admin/api/media/images/${mediaId}`)
}
