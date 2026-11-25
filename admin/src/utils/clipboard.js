import { ElMessage } from 'element-plus'
import Clipboard from 'clipboard'

function clipboardSuccess() {
  ElMessage({
    message: '拷贝成功',
    type: 'success',
    duration: 1500
  })
}

function clipboardError() {
  ElMessage({
    message: '拷贝失败',
    type: 'error'
  })
}

export default function handleClipboard(text, event) {
  const clipboard = new Clipboard(event.target, {
    text: () => text
  })
  clipboard.on('success', () => {
    clipboardSuccess()
    clipboard.off('error')
    clipboard.off('success')
    clipboard.destroy()
  })
  clipboard.on('error', () => {
    clipboardError()
    clipboard.off('error')
    clipboard.off('success')
    clipboard.destroy()
  })
  clipboard.onClick(event)
}
