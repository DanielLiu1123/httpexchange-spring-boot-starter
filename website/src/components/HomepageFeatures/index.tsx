import clsx from 'clsx';
import Heading from '@theme/Heading';
import styles from './styles.module.css';
import React from "react";

type FeatureItem = {
  title: string;
  Svg?: React.ComponentType<React.ComponentProps<'svg'>>;
  description: React.ReactElement;
};

const FeatureList: FeatureItem[] = [
  {
    title: 'Eliminate Boilerplate Code',
    Svg: null,
    description: (
      <>
        Design for eliminating boilerplate code for <code>@HttpExchange</code> clients, providing Spring Cloud OpenFeign like experience.
      </>
    ),
  },
  {
    title: 'Code Generation',
    Svg: null,
    description: (
      <>
        Generate default server implementations from <code>@HttpExchange</code> interfaces, decoupling the server implementation from the API definition.
      </>
    ),
  },
  {
    title: 'Designed for Extension',
    Svg: null,
    description: (
      <>
        This library is designed to be extended and customized to suit your needs.
      </>
    ),
  },
];

function Feature({title, Svg, description}: FeatureItem) {
  return (
    <div className={clsx('col col--4')}>
      {Svg && <div className="text--center">
        <Svg className={styles.featureSvg} role="img"/>
      </div>}
      <div className="text--center padding-horiz--md">
        <Heading as="h3">{title}</Heading>
        <p>{description}</p>
      </div>
    </div>
  );
}

export default function HomepageFeatures(): JSX.Element {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          {FeatureList.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}
