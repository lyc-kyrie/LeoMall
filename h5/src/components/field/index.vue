<template>
	<div class="md_field" :class="{md_field_hasIcon: !!icon, md_field_isError: isError}">
		<van-icon v-if="icon" :name="icon" class="md_feld_icon"/>
		<div class="md_field_control">
			<input
				:type="type"
				v-bind="$attrs"
				:value="modelValue"
				@input="onInput">
		</div>
		<div>
			<slot name="rightIcon">
				<van-icon :name="rightIcon" @click="rightClick" v-show="modelValue" />
			</slot>
		</div>
	</div>
</template>

<script>
// Vue3：v-model = modelValue/update:modelValue；$listeners 并入 $attrs
export default {
  name: 'md-field',
  inheritAttrs: false,

  props: {
    modelValue: {},
    type: {
      type: String,
      default: 'text'
    },
    rightIcon: String,
    icon: String,
    isError: Boolean
  },

  emits: ['update:modelValue', 'right-click'],

  methods: {
    onInput(event) {
      this.$emit('update:modelValue', event.target.value);
    },
    rightClick(event) {
      this.$emit('right-click', event);
    }
  }
};
</script>


<style lang="scss" scoped>
.md_field {
  position: relative;
  border: 1px solid $border-color;
  border-radius: var(--radius-ctl);
  padding-top: 12px;
  padding-bottom: 12px;
  padding-left: 12px;
  display: table;
  width: 100%;
  box-sizing: border-box;
  background-color: var(--card);
  transition: border-color .2s ease-out, box-shadow .2s ease-out;
  &:focus-within {
    border-color: var(--brand);
    box-shadow: 0 0 0 2px var(--brand-soft);
  }
  > div {
    display: table-cell;
  }
  > .md_field_control {
    padding-right: 10px;
    box-sizing: border-box;
    input {
      border: 0;
      line-height: 14px;
      font-size: 14px;
      width: 100%;
    }
  }

  .md_feld_icon {
    position: absolute;
    top: 50%;
    left: 10px;
    transform: translate(0, -50%);
  }
}

.md_field_hasIcon {
  padding-left: 40px;
}

.md_field_isError {
  color: $red;
  background-color: #fcf5f5;
  border: 1px solid $red;
  input {
    color: $red;
    background-color: #fcf5f5;
  }
  input:-webkit-autofill {
    -webkit-box-shadow: 0 0 0 1000px #fcf5f5 inset !important;
  }
}
</style>