import type { Area } from 'react-easy-crop'

// Matches the mini-program home banner: 702rpx wide (750rpx minus page margins) by 220rpx high.
export const BANNER_ASPECT_RATIO = 351 / 110

export const BANNER_OUTPUT_WIDTH = 1404
export const BANNER_OUTPUT_HEIGHT = Math.round(BANNER_OUTPUT_WIDTH / BANNER_ASPECT_RATIO)

function loadImage(source: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = () => reject(new Error('图片读取失败，请重新选择'))
    image.src = source
  })
}

function getBannerFileName(originalName: string) {
  const baseName = originalName.replace(/\.[^.]+$/, '') || 'banner'
  return `${baseName}-banner.jpg`
}

export async function createBannerCropFile(
  source: string,
  crop: Area,
  originalName: string,
): Promise<File> {
  const image = await loadImage(source)
  const canvas = document.createElement('canvas')
  const context = canvas.getContext('2d')

  if (!context) {
    throw new Error('当前浏览器无法处理图片，请更换浏览器后重试')
  }

  // Never enlarge a crop beyond the source pixels it actually contains.
  const outputScale = Math.min(
    1,
    BANNER_OUTPUT_WIDTH / crop.width,
    BANNER_OUTPUT_HEIGHT / crop.height,
  )
  canvas.width = Math.max(1, Math.floor(crop.width * outputScale))
  canvas.height = Math.max(1, Math.floor(crop.height * outputScale))
  context.fillStyle = '#ffffff'
  context.fillRect(0, 0, canvas.width, canvas.height)
  context.imageSmoothingEnabled = true
  context.imageSmoothingQuality = 'high'

  const sourceLeft = Math.max(0, crop.x)
  const sourceTop = Math.max(0, crop.y)
  const sourceRight = Math.min(image.naturalWidth, crop.x + crop.width)
  const sourceBottom = Math.min(image.naturalHeight, crop.y + crop.height)
  const sourceWidth = sourceRight - sourceLeft
  const sourceHeight = sourceBottom - sourceTop

  if (sourceWidth > 0 && sourceHeight > 0) {
    const scaleX = canvas.width / crop.width
    const scaleY = canvas.height / crop.height

    context.drawImage(
      image,
      sourceLeft,
      sourceTop,
      sourceWidth,
      sourceHeight,
      (sourceLeft - crop.x) * scaleX,
      (sourceTop - crop.y) * scaleY,
      sourceWidth * scaleX,
      sourceHeight * scaleY,
    )
  }

  const blob = await new Promise<Blob>((resolve, reject) => {
    canvas.toBlob(
      (result) => {
        if (result) {
          resolve(result)
          return
        }
        reject(new Error('图片处理失败，请重新选择'))
      },
      'image/jpeg',
      0.9,
    )
  })

  return new File([blob], getBannerFileName(originalName), {
    type: 'image/jpeg',
    lastModified: Date.now(),
  })
}
