import React from 'react' // eslint-disable-line
import { merge } from 'lodash'
import { View } from './'

const classes = {
  base: {
    flxs: '',
    flx: 'Flx(flx1)',
    ov: 'Ov(h)'
  }
}

const Page = ({
  children,
  theme,
  ...props
}) => {
  return (
    <View theme={merge({}, classes, theme)} {...props}>
      {children}
    </View>
  )
}

export default Page