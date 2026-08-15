import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import SearchBox from './SearchBox.vue'

describe('SearchBox', () => {
  it('submits a trimmed search keyword', async () => {
    const wrapper = mount(SearchBox)
    await wrapper.get('input').setValue('  宠物视频  ')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('submit')).toEqual([['宠物视频']])
  })
})
