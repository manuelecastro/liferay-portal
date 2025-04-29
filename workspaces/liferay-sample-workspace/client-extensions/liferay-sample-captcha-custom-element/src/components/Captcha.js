import HCaptcha from '@hcaptcha/react-hcaptcha';
import React from 'react';

const onVerifyCaptcha = (token) => {
	console.log("Verified: " + token)
}

const Captcha = () => {
    return (
		<div>
			<HCaptcha sitekey="44fa4708-cb22-4f22-bf08-64d0e08a1dd0" onVerify={onVerifyCaptcha}/>
		</div>
        
    );
}
 
export default Captcha;