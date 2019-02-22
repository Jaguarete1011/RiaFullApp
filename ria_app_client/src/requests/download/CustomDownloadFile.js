import React, { Component } from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { FileService } from "../service/FileService";
import { getFiles } from "../../actions/filesActions";
import { saveAs } from "file-saver";
import IconButton from "@material-ui/core/IconButton";
import { CloudDownloadSharp } from "@material-ui/icons";

class CustomDownloadFile extends Component {
  constructor(props) {
    super(props);
    this.fileService = new FileService();
    this.state = {
      downloading: false,
      fileName: ""
    };
  }

  downloadFile = () => {
    this.setState({
      downloading: true,
      fileName: this.props.fileName
    });

    const filename = "file.txt";

    let self = this;
    this.fileService
      .getFileFromServer()
      .then(response => {
        console.log("Response", response);
        this.setState({ downloading: false });
        saveAs(response.data, filename);
      })
      .catch(function(error) {
        console.log(error);
        self.setState({ downloading: false });
        if (error.response) {
          console.log("Error", error.response.status, filename);
        } else {
          console.log("Error", error.message);
        }
      });
  };

  render() {
    return (
      <React.Fragment>
        <IconButton onClick={this.downloadFile}>
        <CloudDownloadSharp/>
          {" "}
        </IconButton>
        <label>{this.state.downloading ? "Downloading in progress" : ""}</label>
      </React.Fragment>
    );
  }
}

CustomDownloadFile.propTypes = {
  file_entity: PropTypes.object.isRequired,
  getFiles: PropTypes.func.isRequired
};

export default connect(
  null,
  { getFiles }
)(CustomDownloadFile);
