import type { Area } from 'react-easy-crop'

// Matches the mini-program home banner: 702rpx wide (750rpx minus page margins) by 220rpx high.
export const BANNER_ASPECT_RATIO = 351 / 110

const OUTPUT_WIDTH = 1404
const OUTPUT_HEIGHT = Math.round(OUTPUT_WIDTH / BANNER_ASPECT_RATIO)

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

  canvas.width = OUTPUT_WIDTH
  canvas.height = OUTPUT_HEIGHT
  context.fillStyle = '#ffffff'
  context.fillRect(0, 0, OUTPUT_WIDTH, OUTPUT_HEIGHT)
  context.imageSmoothingEnabled = true
  context.imageSmoothingQuality = 'high'
  context.drawImage(
    image,
    crop.x,
    crop.y,
    crop.width,
    crop.height,
    0,
    0,
    OUTPUT_WIDTH,
    OUTPUT_HEIGHT,
  )

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
