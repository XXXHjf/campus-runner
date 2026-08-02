import type { MediaPurpose } from '../services/media.service'

const MAX_UPLOAD_BYTES = 10 * 1024 * 1024
const MAX_LOCAL_SOURCE_BYTES = 25 * 1024 * 1024
const ICON_MAX_DIMENSION = 512

function loadImage(file: File): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const source = URL.createObjectURL(file)
    const image = new Image()
    image.onload = () => {
      URL.revokeObjectURL(source)
      resolve(image)
    }
    image.onerror = () => {
      URL.revokeObjectURL(source)
      reject(new Error('图片读取失败，请重新选择'))
    }
    image.src = source
  })
}

function canvasToBlob(canvas: HTMLCanvasElement, type: string): Promise<Blob> {
  return new Promise((resolve, reject) => {
    canvas.toBlob((blob) => {
      if (blob) {
        resolve(blob)
        return
      }
      reject(new Error('图片处理失败，请重新选择'))
    }, type)
  })
}

function normalizedName(originalName: string, extension: string) {
  const baseName = originalName.replace(/\.[^.]+$/, '') || 'image'
  return `${baseName}.${extension}`
}

async function prepareCategoryIcon(file: File): Promise<File> {
  const image = await loadImage(file)
  const scale = Math.min(
    1,
    ICON_MAX_DIMENSION / image.naturalWidth,
    ICON_MAX_DIMENSION / image.naturalHeight,
  )
  const canvas = document.createElement('canvas')
  canvas.width = Math.max(1, Math.round(image.naturalWidth * scale))
  canvas.height = Math.max(1, Math.round(image.naturalHeight * scale))
  const context = canvas.getContext('2d')
  if (!context) {
    throw new Error('当前浏览器无法处理图片，请更换浏览器后重试')
  }
  context.imageSmoothingEnabled = true
  context.imageSmoothingQuality = 'high'
  context.drawImage(image, 0, 0, canvas.width, canvas.height)

  const blob = await canvasToBlob(canvas, 'image/png')
  return new File([blob], normalizedName(file.name, 'png'), {
    type: 'image/png',
    lastModified: Date.now(),
  })
}

export async function prepareImageForUpload(
  file: File,
  purpose: MediaPurpose,
): Promise<File> {
  if (file.size > MAX_LOCAL_SOURCE_BYTES) {
    throw new Error('图片过大，请选择较小的图片')
  }

  const prepared = purpose === 'ORDER_CATEGORY_ICON'
    || purpose === 'SECOND_HAND_CATEGORY_ICON'
    ? await prepareCategoryIcon(file)
    : file

  if (prepared.size > MAX_UPLOAD_BYTES) {
    throw new Error('图片过大，请选择较小的图片')
  }
  return prepared
}
