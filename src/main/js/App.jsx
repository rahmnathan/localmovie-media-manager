import {RouterProvider, createBrowserRouter} from "react-router-dom";
import React, { Suspense, lazy } from "react";
import ReactDOM from "react-dom/client";
import { MainPage } from "./MainPage.jsx";

// Lazy load VideoPlayer to reduce initial bundle size
const VideoPlayer = lazy(() =>
    import("./VideoPlayer.jsx").then(module => ({
        default: module.VideoPlayer
    }))
);

const LoadingFallback = () => (
    <div style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        height: '100vh',
        color: 'white',
        fontSize: '18px'
    }}>
        Loading video player...
    </div>
);

const router = createBrowserRouter([
    {
        path: "/",
        element: <MainPage/>,
    },
    {
        path: "/play/:mediaId",
        element: (
            <Suspense fallback={<LoadingFallback />}>
                <VideoPlayer/>
            </Suspense>
        )
    }
]);



ReactDOM.createRoot(document.getElementById("react")).render(
    <React.StrictMode>
        <RouterProvider router={router} />
    </React.StrictMode>
);
