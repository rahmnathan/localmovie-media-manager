import React from 'react';
import ReactDOM from 'react-dom';
import { VideoPlayer, viewingVideos } from "./VideoPlayer.jsx";
import { Router, navigate } from "@reach/router"
import { MainPage } from './MainPage.jsx';

class App extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            mediaPath: 'Movies',
            videoPath: ''
        };

        this.setPath = this.setPath.bind(this);
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
                <MainPage path='/' setPath={this.setPath} mediaPath={this.state.mediaPath}/>
                <VideoPlayer path='/play' videoPath={this.state.videoPath}/>
            </Router>
        );
    }
}

ReactDOM.render(
    <App/>,
    document.getElementById('react')
);
