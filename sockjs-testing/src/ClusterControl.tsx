import React, { useState } from 'react';

const ValueControl = (props: {key: string, buttonLabel: string, defaultVal: string}) => {
  const [val, setVal] = useState(props.defaultVal);
  return <div style={{display:"flex", flexDirection:"row"}}>
    <input onChange={(e) => {setVal(e.target.value)}} value={val} type="text"/>
    <button onClick={() => {fetch("/setval", {method: "POST", body: JSON.stringify({key: props.key, value: val})})}}>{props.buttonLabel}</button>
  </div>
}

const ClusterControl = () => {
  return <>
    <ValueControl key="p2cc/radius" buttonLabel="Change Radius" defaultVal="10" />
    <ValueControl key="p2cc/decay" buttonLabel="Change Decay" defaultVal="0.898" />
    <ValueControl key="p2cc/lambda" buttonLabel="Change Lambda" defaultVal="1" />
    <ValueControl key="cc2c/xi" buttonLabel="Change XI" defaultVal="0" />
    <ValueControl key="cc2c/tau" buttonLabel="Change Tau" defaultVal="10" />
    <ValueControl key="gen/pointDelay" buttonLabel="Change Point Delay" defaultVal="100" />
  </>
}
export default ClusterControl;