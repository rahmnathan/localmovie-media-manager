import { usePromiseTracker } from "react-promise-tracker";
import Loader, {ColorRing} from 'react-loader-spinner';
import React from "react";

export const LoadingIndicator = props => {
    const { promiseInProgress } = usePromiseTracker();

    return promiseInProgress === true ? (
        <div
          style={{
            width: "100%",
                height: "100",
                display: "flex",
                justifyContent: "center",
                alignItems: "center"
              }}>
            <ColorRing
                visible={true}
                height="80"
                width="80"
                ariaLabel="blocks-loading"
                wrapperStyle={{}}
                wrapperClass="blocks-wrapper"
                colors={['#b81d2a', '#981b39', '#620616', '#480408', '#860419']}
            />
        </div>) : null;
};