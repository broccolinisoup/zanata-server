import React, { Component, PropTypes } from 'react'
import { isEqual } from 'lodash'

import {
  ButtonLink,
  ButtonRound,
  EditableText,
  Icon,
  LoaderText,
  TableCell,
  TableRow
} from '../../components'
import EntryModal from './EntryModal'
import DeleteEntryModal from './DeleteEntryModal'

class Entry extends Component {
  constructor () {
    super()
    this.state = {
      showEntryModal: false,
      showDeleteModal: false
    }
  }

  handleEntryModalDisplay (display) {
    this.setState({
      showEntryModal: display
    })
  }

  handleDeleteEntryDisplay (display) {
    this.setState({
      showDeleteModal: display
    })
  }

  shouldComponentUpdate (nextProps, nextState) {
    return !isEqual(this.props, nextProps) || !isEqual(this.state, nextState)
  }

  render () {
    const {
      entry,
      handleSelectTerm,
      handleTermFieldUpdate,
      handleDeleteTerm,
      handleResetTerm,
      handleUpdateTerm,
      isSaving,
      isDeleting,
      permission,
      selectedTransLocale,
      selected,
      termsLoading
    } = this.props

    const transContent = entry && entry.transTerm
      ? entry.transTerm.content : ''
    const transSelected = !!selectedTransLocale

    if (!entry) {
      return (
        <TableRow>
          <TableCell>
            <div className='LineClamp(1,24px) Px(rq)'>Loading…</div>
          </TableCell>
        </TableRow>
      )
    }

    const isTermModified = transSelected
      ? (entry.status && entry.status.isTransModified)
      : (entry.status && entry.status.isSrcModified)
    const displayUpdateButton = permission.canUpdateEntry &&
      isTermModified && selected
    const editable = permission.canUpdateEntry && !isSaving

    const updateButton = displayUpdateButton && (
      <ButtonRound atomic={{m: 'Mend(rh)'}}
        type='primary'
        disabled={isSaving}
        onClick={() => handleUpdateTerm(entry)}>
        <LoaderText loading={isSaving} loadingText='Updating'>
          Update
        </LoaderText>
      </ButtonRound>
    )

    const loadingDiv = (
      <div className='LineClamp(1,24px) Px(rq)'>Loading…</div>
    )

    return (
      <TableRow highlight
        className='editable'
        selected={selected}
        onClick={() => handleSelectTerm(entry.id)}>
        <TableCell size='3' tight>
          {termsLoading
            ? loadingDiv
            : (<EditableText
              title={entry.srcTerm.content}
              editable={false}
              editing={selected}>
              {entry.srcTerm.content}
            </EditableText>)
          }
        </TableCell>
        <TableCell size={'3'} tight={transSelected}>
          {termsLoading
            ? loadingDiv
            : transSelected
              ? (<EditableText
                title={transContent}
                editable={transSelected && editable}
                editing={selected}
                onChange={(e) => handleTermFieldUpdate('locale', e)}
                placeholder='Add a translation…'
                emptyReadOnlyText='No translation'>
                {transContent}
              </EditableText>)
              : (<div className='LineClamp(1,24px) Px(rq)'>
                {entry.termsCount}
              </div>)
          }
        </TableCell>
        <TableCell hideSmall>
          {termsLoading
            ? loadingDiv
            : (<EditableText
              title={entry.pos}
              editable={!transSelected && editable}
              editing={selected}
              onChange={(e) => handleTermFieldUpdate('pos', e)}
              placeholder='Add part of speech…'
              emptyReadOnlyText='No part of speech'>
              {entry.pos}
            </EditableText>)
          }
        </TableCell>
        <TableCell size='2'>
          <ButtonLink atomic={{m: 'Mend(rq)'}}
            onClick={() => this.handleEntryModalDisplay(true)}>
            <Icon name='info'/>
          </ButtonLink>
          <EntryModal entry={entry}
            show={this.state.showEntryModal}
            isSaving={isSaving}
            selectedTransLocale={selectedTransLocale}
            canUpdate={displayUpdateButton}
            handleEntryModalDisplay={(display) =>
              this.handleEntryModalDisplay(display)}
            handleResetTerm={(entryId) => handleResetTerm(entryId)}
            handleTermFieldUpdate={(field, e) =>
              handleTermFieldUpdate(field, e)}
            handleUpdateTerm={(entry) => handleUpdateTerm(entry)}
          />

          {updateButton}
          <div className='Op(0) row--selected_Op(1) editable:h_Op(1) Trs(eo)'>
            {displayUpdateButton && !isSaving ? (
              <ButtonLink
                onClick={() => handleResetTerm(entry.id)}>
                Cancel
              </ButtonLink>
            ) : ''
            }

            {!transSelected && permission.canDeleteEntry && !isSaving &&
              !displayUpdateButton && (
              <DeleteEntryModal entry={entry}
                isDeleting={isDeleting}
                show={this.state.showDeleteModal}
                handleDeleteEntryDisplay={(display) =>
                  this.handleDeleteEntryDisplay(display)}
                handleDeleteEntry={handleDeleteTerm}/>)
            }
          </div>
        </TableCell>
      </TableRow>
    )
  }
}

Entry.propTypes = {
  entry: PropTypes.object,
  handleSelectTerm: PropTypes.func,
  handleTermFieldUpdate: PropTypes.func,
  handleDeleteTerm: PropTypes.func,
  handleResetTerm: PropTypes.func,
  handleUpdateTerm: PropTypes.func,
  index: PropTypes.number,
  isSaving: PropTypes.bool,
  isDeleting: PropTypes.bool,
  permission: PropTypes.object,
  selectedTransLocale: PropTypes.object,
  selected: PropTypes.bool,
  termsLoading: PropTypes.bool
}

export default Entry
