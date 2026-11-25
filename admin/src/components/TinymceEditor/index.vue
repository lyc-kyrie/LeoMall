<template>
  <div class="tinymce-editor">
    <textarea :id="tid" />
  </div>
</template>

<script>
// 阶段2D-4a：自绘 Vue3 wrapper 替代 @tinymce/tinymce-vue（core 仍走 public 全局脚本 tinymce4.7.5）
let seq = 0

export default {
  name: 'TinymceEditor',
  props: {
    modelValue: { type: String, default: '' },
    init: { type: Object, default: () => ({}) }
  },
  emits: ['update:modelValue'],
  data() {
    return {
      tid: 'tinymce_editor_' + (++seq),
      editor: null
    }
  },
  watch: {
    modelValue(v) {
      if (this.editor && v !== this.editor.getContent()) {
        this.editor.setContent(v || '')
      }
    }
  },
  mounted() {
    const tinymce = window.tinymce
    if (!tinymce) return
    const conf = Object.assign({}, this.init, {
      selector: '#' + this.tid,
      setup: (ed) => {
        this.editor = ed
        ed.on('init', () => {
          ed.setContent(this.modelValue || '')
        })
        const emit = () => this.$emit('update:modelValue', ed.getContent())
        ed.on('change', emit)
        ed.on('keyup', emit)
        ed.on('undo', emit)
        ed.on('redo', emit)
      }
    })
    tinymce.init(conf)
  },
  beforeUnmount() {
    if (window.tinymce && this.editor) {
      window.tinymce.remove(this.editor)
    }
  }
}
</script>