import React, { Component } from 'react'
import Helmet from 'react-helmet'
import { connect } from 'react-redux'
import { hashHistory } from 'react-router'
import { isEmpty, values } from 'lodash'
import { canGoBack } from '../../utils/RoutingHelpers'
import {
  Base,
  Page,
  ScrollView,
  View,
  Heading,
  Icon,
  Button,
  TextInput,
  TeaserList
} from '../../components'
import {
  searchTextChanged,
  searchPageLoaded
} from '../../actions/explore'

const headerClasses = {
  ai: 'Ai(c)',
  bxsh: 'Bxsh(shw)',
  bxz: 'Bxz(cb)', // For chrome bug that doesn't keep height of container
  d: 'D(f)',
  fz: 'Fz(ms1)--md',
  jc: 'Jc(c)',
  p: 'Py(rq) Px(rh) P(r1)--sm',
  pos: 'Pos(r)'
}
const headingTheme = {
  base: {
    hidden: 'Hidden'
  }
}
const searchViewTheme = {
  base: {
    ai: 'Ai(c)',
    c: 'C(dark)',
    fld: '',
    pos: 'Pos(r)',
    maw: 'Maw(r32)',
    w: 'W(100%)'
  }
}
const iconClasses = {
  ai: 'Ai(c)',
  c: 'C(neutral)',
  fz: 'Fz(ms1) Fz(ms2)--sm',
  jc: 'Jc(c)',
  h: 'H(100%)',
  l: 'Start(rq) Start(rh)--md',
  m: 'Mstart(re) Mstart(0)--md',
  pos: 'Pos(a)',
  t: 'T(0)',
  ta: 'Ta(c)',
  w: 'W(ms1) W(ms2)--md'
}
const inputTheme = {
  base: {
    bdrs: 'Bdrs(rnd)',
    p: 'Py(rq) Py(rh)--md Pstart(r1q) Pstart(r1h)--md Pend(rq)',
    w: 'W(100%)'
  }
}
const buttonTheme = {
  base: {
    c: 'C(pri)',
    m: 'Mstart(rq)'
  }
}
const scrollViewTheme = {
  base: {
    ai: 'Ai(c)'
  }
}
const contentViewContainerTheme = {
  base: {
    maw: 'Maw(r32)',
    m: 'Mx(a)',
    w: 'W(100%)'
  }
}

const titles = {
  project: 'Projects',
  languageTeam: 'Language Teams',
  person: 'People'
}

class Explore extends Component {
  componentWillMount () {
    this.props.handleSearchPageLoad()
  }
  render () {
    const {
      handleSearchCancelClick,
      handleSearchTextChange,
      searchText,
      searchResults = {},
      searchError,
      searchLoading,
      ...props
    } = this.props
    const searchEntities = searchResults.entities || {}
    return (
      <Page>
        <Helmet title='Search' />
        <Base tagName='header' theme={headerClasses}>
          <Heading level='1' theme={headingTheme}>Search</Heading>
          <View theme={searchViewTheme}>
            <Icon name='search' atomic={iconClasses}/>
            <TextInput
              autoFocus
              type='search'
              placeholder='Search Zanata…'
              accessibilityLabel='Search Zanata'
              theme={inputTheme}
              value={searchText}
              onChange={handleSearchTextChange}
            />
            <Button
              theme={buttonTheme}
              onClick={handleSearchCancelClick}>
            Cancel
            </Button>
          </View>
        </Base>
        <ScrollView theme={scrollViewTheme}>
          <View theme={contentViewContainerTheme}>
            {isEmpty(searchEntities)
              ? searchLoading
                ? (<div>Loading results…</div>)
                : searchError
                  ? (<p>
                      Error completing search for "{searchText}".<br/>
                    {searchResults.message}. Please try again.
                  </p>)
                  : (<p>No Results</p>)
              : Object.keys(searchEntities).map((type, key) =>
                (<TeaserList
                  items={values(searchEntities[type])}
                  title={titles[type]}
                  type={type}
                  key={key}
                  filterable={!searchText}
                />)
              )
            }
          </View>
        </ScrollView>
      </Page>
    )
  }
}

const mapStateToProps = (state) => {
  return {
    location: state.routing.location,
    searchText: state.routing.location.query.q,
    searchResults: state.routing.location.query.q
      ? state.explore.results : state.explore.default,
    searchError: state.explore.error,
    searchLoading: state.explore.loading
  }
}

const mapDispatchToProps = (dispatch, {
  history
}) => {
  return {
    handleSearchCancelClick: () => {
      console.log(hashHistory)
      if (canGoBack) {
        dispatch(hashHistory.goBack())
      } else {
        dispatch(hashHistory.push('/'))
      }
    },
    handleSearchTextChange: (event) => {
      dispatch(searchTextChanged(event.target.value))
    },
    handleSearchPageLoad: () => {
      dispatch(searchPageLoaded())
    }
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(Explore)