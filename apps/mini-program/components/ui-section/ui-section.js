Component({
  options: {
    multipleSlots: true
  },
  properties: {
    title: {
      type: String,
      value: ""
    },
    description: {
      type: String,
      value: ""
    },
    icon: {
      type: String,
      value: ""
    },
    plain: {
      type: Boolean,
      value: false
    }
  }
})
