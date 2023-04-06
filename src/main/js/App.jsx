import {RouterProvider, createBrowserRouter} from "react-router-dom";
import React from "react";
import ReactDOM from "react-dom/client";
import { MainPage } from "./MainPage.jsx";

const router = createBrowserRouter([
    {
        path: "/",
        element: <MainPage/>,
    }
]);


ReactDOM.createRoot(document.getElementById("react")).render(
    <React.StrictMode>
        <RouterProvider router={router} />
    </React.StrictMode>
);
