import {RouterProvider, createBrowserRouter} from "react-router-dom";
import React from "react";
import ReactDOM from "react-dom/client";
import { MainPage } from "./MainPage.jsx";

import {VideoPlayer} from "./VideoPlayer.jsx";

const router = createBrowserRouter([
    {
        path: "/",
        element: <MainPage/>,
    },
    {
        path: "/play/:mediaId",
        element: <VideoPlayer/>
    }
]);



ReactDOM.createRoot(document.getElementById("react")).render(
    <React.StrictMode>
        <RouterProvider router={router} />
    </React.StrictMode>
);
