import React from 'react';
import ReactDOM from 'react-dom';
import { VideoPlayer, viewingVideos } from "./VideoPlayer.jsx";
import { Router, navigate } from "@reach/router";
import { MainPage } from './MainPage.jsx';
import { LoadingIndicator } from './LoadingIndicator.jsx';

class App extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            mediaPath: 'Movies',
            videoPath: '',
            videoStartPercent: 0
        };

        this.setPathAndStartPercent = this.setPathAndStartPercent.bind(this);
        this.setPath = this.setPath.bind(this);
    }

    setPathAndStartPercent(path, startPosition) {
        if(path !== null){
            if(viewingVideos(path)){
                this.setState( { videoPath: path, videoStartPercent: startPosition });
                navigate('/play');
            } else {
                this.setState({ mediaPath: path });
            }
        }
    }

    setPath(path) {
        if(path !== null){
            if(viewingVideos(path)){
                this.setState( { videoPath: path });
                navigate('/play');
            } else {
                this.setState({ mediaPath: path });
            }
        }
    }

    render(){
        return(
            <Router>
                <MainPage path='/' setPathAndStartPercent={this.setPathAndStartPercent} mediaPath={this.state.mediaPath} setPath={this.setPath}/>
                <VideoPlayer path='/play' videoPath={this.state.videoPath} videoStartPercent={this.state.videoStartPercent}/>
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
