import React from 'react';
import ReactDOM from 'react-dom';
import { VideoPlayer } from "./VideoPlayer.jsx";
import { Router, navigate } from "@reach/router";
import { MainPage } from './MainPage.jsx';
import { LoadingIndicator } from './LoadingIndicator.jsx';

class App extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            mediaPath: 'Movies',
            videoPath: '',
            mediaFile: '',
            startAtBeginning: false
        };

        this.selectMediaFile = this.selectMediaFile.bind(this);
        this.setPath = this.setPath.bind(this);
        this.playMedia = this.playMedia.bind(this);
    }

    selectMediaFile(mediaFile) {
        this.setState({mediaPath: mediaFile.path});
        navigate('?path=' + encodeURIComponent(mediaFile.path))
    }

    playMedia(mediaFile, startAtBeginning) {
        this.setState({ startAtBeginning: startAtBeginning })
        navigate('/play/' + mediaFile.mediaFileId);
    }

    setPath(path) {
        if (path !== null) {
            this.setState({mediaPath: path});
            navigate('?path=' + encodeURIComponent(path))
        }
    }

    render(){
        return(
            <Router>
                <MainPage path='/' selectMediaFile={this.selectMediaFile} mediaPath={this.state.mediaPath} setPath={this.setPath} playMedia={this.playMedia}/>
                <VideoPlayer path='/play/:mediaFileId' mediaFile={this.state.mediaFile} startAtBeginning={this.state.startAtBeginning}/>
            </Router>
        );
    }
}

ReactDOM.render(
    <div>
        <App/>
        <LoadingIndicator/>
    </div>,
    document.getElementById('react')
);
