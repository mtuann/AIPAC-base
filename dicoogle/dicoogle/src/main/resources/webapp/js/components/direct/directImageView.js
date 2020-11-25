import React from "react";
import { getUrlVars } from "../../utils/url";
import { Endpoints } from "../../constants/endpoints";

export const DirectImageView = React.createClass({
  propTypes: {
    params: React.PropTypes.object.isRequired
  },

  getInitialState: function() {
    return { error: false };
  },

  render() {
    const instanceUid = this.props.params.uid || getUrlVars().SOPInstanceUID;
    const url = Endpoints.base + "/dic2png?SOPInstanceUID=" + instanceUid;

    return this.state.error ? (
      <img src="assets/image-not-found.png" width="auto" height="300px" />
    ) : (
      <img src={url} width="100%" height="100%" onError={this.imageLoadError} />
    );
  },

  imageLoadError() {
    this.setState({ error: true });
  }
});
