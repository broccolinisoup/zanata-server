import { isUndefined, filter, forOwn } from 'lodash'
import { trim } from './StringUtils'
import { isEmpty } from 'lodash'
import DateHelpers from './DateHelper'
import defined from 'defined'

var GlossaryHelper = {
  /**
   * Generate org.zanata.rest.dto.GlossaryTerm object
   * returns undefined if data is undefined or locale is empty
   *
   * @param data
   */
  generateTermDTO: function (data, trimContent) {
    if (isUndefined(data) || isEmpty(data.locale)) {
      return
    }
    const comment = isEmpty(data.content) ? undefined : trim(data.comment)
    return {
      content: trimContent ? trim(data.content) : data.content,
      locale: data.locale,
      comment: comment
    }
  },

  /**
   * Generate org.zanata.rest.dto.GlossaryEntry object
   * @param data
   */
  convertToDTO: function (data) {
    var entry = {}

    entry.id = data.id
    entry.pos = trim(data.pos)
    entry.description = trim(data.description)
    entry.srcLang = data.srcTerm.locale
    entry.sourceReference = data.srcTerm.reference
    entry.glossaryTerms = []

    var srcTerm = this.generateTermDTO(data.srcTerm, false)
    if (!isUndefined(srcTerm)) {
      entry.glossaryTerms.push(srcTerm)
    }

    var transTerm = this.generateTermDTO(data.transTerm, true)
    if (!isUndefined(transTerm)) {
      entry.glossaryTerms.push(transTerm)
    }
    return [entry]
  },

  generateEmptyTerm: function (localeId) {
    return {
      content: '',
      locale: localeId,
      comment: '',
      lastModifiedDate: '',
      lastModifiedBy: ''
    }
  },

  generateEmptySrcTerm: function (localeId) {
    let term = this.generateEmptyTerm(localeId)
    term.reference = ''
    return term
  },

  getTermByLocale: function (terms, localeId) {
    let term = filter(terms, ['locale', localeId])
    return term.length ? term[0] : undefined
  },

  generateEmptyEntry: function (srcLocaleId) {
    return {
      description: undefined,
      pos: undefined,
      srcTerm: this.generateEmptyTerm(srcLocaleId)
    }
  },

  generateEntry: function (entry, transLocaleId) {
    let srcTerm =
      this.getTermByLocale(entry.glossaryTerms, entry.srcLang)
    srcTerm.reference = entry.sourceReference
    if (!isEmpty(srcTerm.lastModifiedDate)) {
      srcTerm.lastModifiedDate =
        DateHelpers.shortDate(DateHelpers.getDate(srcTerm.lastModifiedDate))
    }

    let transTerm
    if (transLocaleId) {
      transTerm = this.getTermByLocale(entry.glossaryTerms, transLocaleId)
      if (transTerm) {
        transTerm.lastModifiedDate =
          DateHelpers.shortDate(DateHelpers.getDate(transTerm.lastModifiedDate))
        if (isUndefined(transTerm.comment)) {
          transTerm.comment = ''
        }
      } else {
        transTerm = this.generateEmptyTerm(transLocaleId)
      }
    }

    return {
      id: entry.id,
      pos: defined(entry.pos, ''),
      description: defined(entry.description, ''),
      termsCount: entry.termsCount > 0 ? entry.termsCount - 1 : 0,
      srcTerm: srcTerm,
      transTerm: transTerm,
      status: this.getDefaultEntryStatus()
    }
  },

  toEmptyString: (val) => {
    return isEmpty(val) ? '' : val
  },

  getEntryStatus: function (entry, originalEntry) {
    if (entry && originalEntry) {
      const source = this.toEmptyString(entry.srcTerm.content)
      const trans = entry.transTerm
        ? this.toEmptyString(entry.transTerm.content) : ''
      const comment = entry.transTerm
        ? this.toEmptyString(entry.transTerm.comment) : ''
      const desc = this.toEmptyString(entry.description)
      const pos = this.toEmptyString(entry.pos)

      const ori_source = this.toEmptyString(
        originalEntry.srcTerm.content)
      const ori_trans = originalEntry.transTerm
        ? this.toEmptyString(originalEntry.transTerm.content) : ''
      const ori_comment = originalEntry.transTerm
        ? this.toEmptyString(originalEntry.transTerm.comment) : ''
      const ori_desc = this.toEmptyString(originalEntry.description)
      const ori_pos = this.toEmptyString(originalEntry.pos)

      let isSrcModified = (desc !== ori_desc) ||
        (pos !== ori_pos) ||
        (source !== ori_source)
      let isTransModified = (trans !== ori_trans) || (comment !== ori_comment)

      let isSrcValid = !isEmpty(trim(source))

      return {
        isSrcModified: isSrcModified,
        isTransModified: isTransModified,
        isSrcValid: isSrcValid // source content is mandatory
      }
    }
    return this.getDefaultEntryStatus()
  },

  getDefaultEntryStatus: function () {
    return {
      isSrcModified: false,
      isTransModified: false,
      isSrcValid: true
    }
  },

  convertSortToObject: function (sortString) {
    if (!sortString) {
      return {
        src_content: true
      }
    } else {
      let sort = {}
      if (sortString.startsWith('-')) {
        sort[sortString.substring(1, sortString.length)] = false
      } else {
        sort[sortString] = true
      }
      return sort
    }
  },

  convertSortToParam: function (sort) {
    let params = []
    forOwn(sort, function (value, field) {
      let param = (value ? '' : '-') + field
      params.push(param)
    })
    return params.length ? params.join() : ''
  }
}

export default GlossaryHelper
