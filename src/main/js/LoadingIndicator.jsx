import { usePromiseTracker } from "react-promise-tracker";
import Loader from 'react-loader-spinner';
import React from "react";

export const LoadingIndicator = props => {
    const { promiseInProgress } = usePromiseTracker();

    return promiseInProgress &&
        <div
          style={{
            width: "100%",
                height: "100",
                display: "flex",
                justifyContent: "center",
                alignItems: "center"
              }}>
            <Loader type="ThreeDots" color="#FF0000" height="100" width="100" />
        </div>
};