import React, { Component, PropTypes } from 'react'
import { connect } from 'react-redux'
import {
  ButtonRound,
  Icon,
  Modal,
  Row
} from './'
import {
  clearMessage
} from '../actions/common'
import { isEmptyOrNull } from '../utils/StringUtils'

class Notification extends Component {

  getSeverityClass (severity) {
    switch (severity) {
      case 'warn':
        return 'C(danger)'
      case 'error':
        return 'C(danger)'
      default:
        return ''
    }
  }

  getIcon (severity) {
    switch (severity) {
      case 'warn':
      case 'error':
        return 'warning'
      default:
        return 'info'
    }
  }

  clearMessage () {
    this.props.handleClearMessage()
  }

  render () {
    const {
      severity,
      message,
      details,
      show
      } = this.props
    const severityClass = this.getSeverityClass(severity)
    const icon = this.getIcon(severity)

    return (
      <Modal show={show} onHide={() => this.clearMessage()}>
        <Modal.Header>
          <Modal.Title>
            <Row theme={{ base: {W: 'W(100%)', C: severityClass, Jc: 'Jc(c)'} }}>
              <Icon name={icon} atomic={{m: 'Mend(re)'}} size='2' />
              <span>Notification</span>
            </Row>
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <div className={severityClass + ' My(rh)'}>
            {message}
          </div>
          {!isEmptyOrNull(details)
            ? (<div className='Brds(rq) Bxsh(sh1) P(rh) Fz(msn1)'>
                  {details}
                </div>)
            : undefined}
        </Modal.Body>
        <Modal.Footer>
          <ButtonRound type='primary' onClick={() => this.clearMessage()}>
            Close
          </ButtonRound>
        </Modal.Footer>
      </Modal>
    )
  }
}

Notification.propType = {
  severity: PropTypes.string.isRequired,
  message: PropTypes.string,
  details: PropTypes.object,
  show: PropTypes.bool
}

const mapDispatchToProps = (dispatch) => {
  return {
    dispatch,
    handleClearMessage: (termId) => dispatch(clearMessage())
  }
}

export default connect(null, mapDispatchToProps)(Notification)