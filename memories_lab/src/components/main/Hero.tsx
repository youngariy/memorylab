import { motion } from 'framer-motion';
import styles from './Hero.module.css';
import arrowRight from '@/assets/arrow_right.svg';

function Hero() {
  return (
    <section className={styles.hero}>
      <div className={styles.starsBackground}>
        <div className={styles.stars}></div>
        <div className={styles.starsSecond}></div>
        <div className={styles.starsThird}></div>
      </div>
      <div className={styles.container}>
        <motion.div
          className={styles.content}
          initial={{ y: 50, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          transition={{ duration: 0.8, delay: 0.2 }}
        >
          <motion.span
            className={styles.subtitle}
            initial={{ y: 20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ duration: 0.6, delay: 0.4 }}
          >
            <span>PERSBOL</span>
            <img
              src={arrowRight}
              alt="arrowRight"
              className={styles.arrowRight}
            />
          </motion.span>

          <motion.h1
            className={styles.title}
            initial={{ y: 30, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ duration: 0.8, delay: 0.6 }}
          >
            사진 이상의 가치
          </motion.h1>

          <motion.p
            className={styles.description}
            initial={{ y: 20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ duration: 0.6, delay: 0.8 }}
          >
            단 한번의 업로드로,
            <br />
            상상 속 추억이 살아 숨 쉬는 3D 메타버스로 완성됩니다.
          </motion.p>
        </motion.div>
      </div>
    </section>
  );
}

export default Hero;
